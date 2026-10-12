package com.example.motorapp.data

import android.util.Log
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@Serializable
private data class RoomInsertDto(
    val id: String,
    val type: RoomType,
    val name: String? = null
)

@Serializable
private data class ParticipantInsertDto(
    @SerialName("room_id") val roomId: String,
    @SerialName("user_id") val userId: String,
    val role: String? = "member"
)

@Serializable
private data class ParticipantRecordDto(
    @SerialName("room_id") val roomId: String = "",
    @SerialName("user_id") val userId: String = "",
    val role: String? = "member"
)

@Serializable
data class RoomSubtitleInfo(
    val lastMessageText: String? = null,
    val isLastFromMe: Boolean = false,
    val unreadCount: Int = 0
)

@Serializable
private data class UpdateReadDto(
    @SerialName("is_read") val isRead: Boolean = true
)

@Serializable
private data class UpdateConvoyRideStatusDto(
    @SerialName("is_active") val isActive: Boolean = false
)

@Serializable
private data class UpsertConvoyRiderLocationDto(
    @SerialName("ride_id") val rideId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("has_joined") val hasJoined: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: Float = 0f,
    @SerialName("speed_kmh") val speedKmh: Float = 0f,
    @SerialName("updated_at") val updatedAt: String
)

private fun getIsoUtcTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

class ChatRepository {

    val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    /**
     * Real-time stream of messages for a single active room.
     */
    @OptIn(SupabaseExperimental::class)
    fun getMessagesStream(roomId: String): Flow<List<ChatMessage>> {
        return supabase.from("chat_messages")
            .selectAsFlow(
                ChatMessage::id,
                filter = FilterOperation("room_id", FilterOperator.EQ, roomId)
            )
            .map { list ->
                list.sortedBy { it.createdAt ?: "" }
            }
    }

    /**
     * Real-time stream to power background/in-app notifications for incoming messages
     * across rooms where the sender is someone else.
     */
    @OptIn(SupabaseExperimental::class)
    fun getAllIncomingMessagesStream(): Flow<List<ChatMessage>> {
        val uid = currentUserId ?: return emptyFlow()
        return supabase.from("chat_messages")
            .selectAsFlow(
                ChatMessage::id,
                filter = FilterOperation("sender_id", FilterOperator.NEQ, uid)
            )
    }

    /**
     * Preview info: detects if the last message was sent by the current user ("You: ...")
     * or by the other party with unread message counts.
     */
    suspend fun getDirectRoomPreview(roomId: String): RoomSubtitleInfo = runCatching {
        val uid = currentUserId ?: return@runCatching RoomSubtitleInfo()
        val messages = supabase.from("chat_messages")
            .select {
                filter { eq("room_id", roomId) }
            }
            .decodeList<ChatMessage>()
            .sortedBy { it.createdAt ?: "" }

        if (messages.isEmpty()) {
            return@runCatching RoomSubtitleInfo(lastMessageText = null, isLastFromMe = false, unreadCount = 0)
        }

        val last = messages.last()
        val isFromMe = last.senderId == uid
        val unreadCount = messages.count { it.senderId != uid && !it.isRead }

        RoomSubtitleInfo(
            lastMessageText = last.content,
            isLastFromMe = isFromMe,
            unreadCount = unreadCount
        )
    }.getOrDefault(RoomSubtitleInfo())

    /**
     * Marks all incoming messages from others in the room as read.
     */
    suspend fun markMessagesAsRead(roomId: String): Result<Unit> = runCatching {
        val uid = currentUserId ?: return@runCatching
        supabase.from("chat_messages").update(UpdateReadDto(isRead = true)) {
            filter {
                eq("room_id", roomId)
                neq("sender_id", uid)
                eq("is_read", false)
            }
        }
    }

    /**
     * Returns the user profiles of all participants joined in a group / convoy room.
     */
    suspend fun getGroupMembers(roomId: String): Result<List<UserProfile>> = runCatching {
        Log.d("ConvoyDebug", "--> Querying participants for roomId: '$roomId'")

        val participants = supabase.from("chat_participants")
            .select {
                filter { eq("room_id", roomId) }
            }
            .decodeList<ParticipantRecordDto>()

        Log.d("ConvoyDebug", "--> Raw participants found in DB: ${participants.size}")
        participants.forEach {
            Log.d("ConvoyDebug", "    participant: roomId=${it.roomId}, userId=${it.userId}, role=${it.role}")
        }

        val userIds = participants.map { it.userId }.filter { it.isNotBlank() }.distinct()
        if (userIds.isEmpty()) {
            Log.w("ConvoyDebug", "--> userIds list is empty!")
            return@runCatching emptyList()
        }

        val profiles = try {
            supabase.from("profiles")
                .select {
                    filter { isIn("id", userIds) }
                }
                .decodeList<UserProfile>()
        } catch (e: Exception) {
            Log.e("ConvoyDebug", "--> Failed decoding profiles table, using fallback cards", e)
            emptyList()
        }

        Log.d("ConvoyDebug", "--> Profiles successfully loaded: ${profiles.size}")

        if (profiles.isNotEmpty()) {
            profiles
        } else {
            Log.w("ConvoyDebug", "--> Profiles returned 0 rows. Using fallback placeholder cards.")
            userIds.map { uid ->
                UserProfile(
                    id = uid,
                    username = if (uid == currentUserId) "You (Admin/Leader)" else "Rider (${uid.take(5)})",
                    bikeName = "Convoy Participant"
                )
            }
        }
    }.onFailure { err ->
        Log.e("ConvoyDebug", "--> getGroupMembers fatal error", err)
    }

    /**
     * Returns the actual count of members joined in a convoy / group room.
     */
    suspend fun getRoomMemberCount(roomId: String): Int = runCatching {
        val count = supabase.from("chat_participants")
            .select {
                filter { eq("room_id", roomId) }
                count(io.github.jan.supabase.postgrest.query.Count.EXACT)
            }
            .countOrNull() ?: 1
        count.toInt()
    }.getOrDefault(1)

    /**
     * Returns the unread messages count sent by other riders in a room.
     */
    suspend fun getUnreadCount(roomId: String): Int = runCatching {
        val uid = currentUserId ?: return@runCatching 0
        val messages = supabase.from("chat_messages")
            .select {
                filter {
                    eq("room_id", roomId)
                    neq("sender_id", uid)
                    eq("is_read", false)
                }
            }
            .decodeList<ChatMessage>()
        messages.size
    }.getOrDefault(0)

    /**
     * Sends a message to a specific room.
     */
    suspend fun sendMessage(roomId: String, content: String): Result<Unit> = runCatching {
        val uid = currentUserId ?: error("User not authenticated")
        val payload = SendMessageDto(
            roomId = roomId,
            senderId = uid,
            content = content.trim()
        )
        supabase.from("chat_messages").insert(payload)
    }

    /**
     * Search riders by username in the profiles table.
     * When query is blank, loads default/recent riders so the dialog is populated.
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> = runCatching {
        val myId = currentUserId ?: ""
        val trimmed = query.trim()

        supabase.from("profiles")
            .select {
                filter {
                    if (trimmed.isNotBlank()) {
                        ilike("username", "%$trimmed%")
                    }
                    if (myId.isNotBlank()) {
                        neq("id", myId)
                    }
                }
                limit(25)
            }
            .decodeList<UserProfile>()
    }

    /**
     * Finds an existing direct room or creates a new one between current user and target user.
     */
    suspend fun createOrGetDirectChat(targetUser: UserProfile): Result<ChatRoom> = runCatching {
        val myId = currentUserId ?: error("User not authenticated")

        val myParticipations = supabase.from("chat_participants")
            .select {
                filter { eq("user_id", myId) }
            }
            .decodeList<ParticipantRecordDto>()

        if (myParticipations.isNotEmpty()) {
            val myRoomIds = myParticipations.map { it.roomId }
            val existingDirects = supabase.from("chat_rooms")
                .select {
                    filter {
                        isIn("id", myRoomIds)
                        eq("type", RoomType.DIRECT.name.lowercase())
                    }
                }
                .decodeList<ChatRoom>()

            for (room in existingDirects) {
                val partner = supabase.from("chat_participants")
                    .select {
                        filter {
                            eq("room_id", room.id)
                            eq("user_id", targetUser.id)
                        }
                    }
                    .decodeList<ParticipantRecordDto>()

                if (partner.isNotEmpty()) {
                    return@runCatching room.copy(name = targetUser.username ?: "Direct Chat")
                }
            }
        }

        val newRoomId = UUID.randomUUID().toString()
        val roomTitle = targetUser.username ?: "Direct Chat"

        supabase.from("chat_rooms").insert(
            RoomInsertDto(id = newRoomId, type = RoomType.DIRECT, name = roomTitle)
        )

        val participants = listOf(
            ParticipantInsertDto(roomId = newRoomId, userId = myId),
            ParticipantInsertDto(roomId = newRoomId, userId = targetUser.id)
        )
        supabase.from("chat_participants").insert(participants)

        ChatRoom(id = newRoomId, type = RoomType.DIRECT, name = roomTitle)
    }

    /**
     * Creates a new Group / Convoy room with selected riders.
     */
    suspend fun createGroupChat(groupName: String, selectedUserIds: List<String>): Result<ChatRoom> = runCatching {
        val myId = currentUserId ?: error("User not authenticated")
        val newRoomId = UUID.randomUUID().toString()

        supabase.from("chat_rooms").insert(
            RoomInsertDto(id = newRoomId, type = RoomType.CONVOY, name = groupName.trim())
        )

        val participants = mutableListOf(
            ParticipantInsertDto(roomId = newRoomId, userId = myId, role = "leader")
        )
        selectedUserIds.distinct().forEach { uid ->
            if (uid != myId) {
                participants.add(ParticipantInsertDto(roomId = newRoomId, userId = uid, role = "member"))
            }
        }
        supabase.from("chat_participants").insert(participants)

        ChatRoom(id = newRoomId, type = RoomType.CONVOY, name = groupName.trim())
    }

    /**
     * Fast one-shot fetch for initial screen render (ordered & limited).
     */
    suspend fun getRecentMessages(roomId: String, limit: Long = 50): Result<List<ChatMessage>> = runCatching {
        supabase.from("chat_messages")
            .select {
                filter { eq("room_id", roomId) }
                order("created_at", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                limit(limit)
            }
            .decodeList<ChatMessage>()
    }

    /**
     * Verifies if the current user is the leader/creator of this chat room.
     */
    suspend fun isRoomAdmin(roomId: String): Boolean = runCatching {
        val uid = currentUserId ?: return@runCatching false
        val participant = supabase.from("chat_participants")
            .select {
                filter {
                    eq("room_id", roomId)
                    eq("user_id", uid)
                }
            }
            .decodeSingleOrNull<ParticipantRecordDto>()

        participant?.role.equals("leader", ignoreCase = true)
    }.getOrDefault(false)

    /**
     * Fetch rooms that the current user belongs to or can access.
     */
    suspend fun getAvailableRooms(type: RoomType): Result<List<ChatRoom>> = runCatching {
        val myId = currentUserId ?: ""

        when (type) {
            RoomType.REGIONAL -> {
                supabase.from("chat_rooms")
                    .select {
                        filter { eq("type", RoomType.REGIONAL.name.lowercase()) }
                    }
                    .decodeList<ChatRoom>()
            }
            RoomType.DIRECT, RoomType.CONVOY -> {
                if (myId.isBlank()) return@runCatching emptyList()

                val userRooms = supabase.from("chat_participants")
                    .select {
                        filter { eq("user_id", myId) }
                    }
                    .decodeList<ParticipantRecordDto>()

                if (userRooms.isEmpty()) return@runCatching emptyList()

                val roomIds = userRooms.map { it.roomId }.distinct()

                supabase.from("chat_rooms")
                    .select {
                        filter {
                            isIn("id", roomIds)
                            eq("type", type.name.lowercase())
                        }
                    }
                    .decodeList<ChatRoom>()
            }
        }
    }

    /**
     * Removes the current user from the chat room so it no longer appears in their list.
     * If the user is the leader/creator of a convoy room, deletes the whole room.
     */
    suspend fun leaveOrDeleteRoom(roomId: String): Result<Unit> = runCatching {
        val uid = currentUserId ?: error("User not authenticated")
        val isAdmin = isRoomAdmin(roomId)

        if (isAdmin) {
            try {
                supabase.from("chat_messages").delete { filter { eq("room_id", roomId) } }
                supabase.from("chat_participants").delete { filter { eq("room_id", roomId) } }
            } catch (e: Exception) {
                Log.w("ChatRepository", "Cascade cleanup skipped or partially failed: ${e.message}")
            }

            supabase.from("chat_rooms").delete {
                filter { eq("id", roomId) }
            }
        } else {
            supabase.from("chat_participants").delete {
                filter {
                    eq("room_id", roomId)
                    eq("user_id", uid)
                }
            }
        }
    }

    // ==========================================
    // Convoy Ride & Live Location Tracking
    // ==========================================

    /**
     * Fast one-shot fetch for active ride to render instantly without waiting for websocket negotiation.
     */
    suspend fun getActiveRide(roomId: String): ConvoyRide? = runCatching {
        supabase.from("convoy_rides")
            .select {
                filter {
                    eq("room_id", roomId)
                    eq("is_active", true)
                }
                limit(1)
            }
            .decodeList<ConvoyRide>()
            .firstOrNull()
    }.getOrNull()

    /**
     * Streams the currently active ride event for this chat room.
     */
    @OptIn(SupabaseExperimental::class)
    fun getActiveRideStream(roomId: String): Flow<ConvoyRide?> {
        return supabase.from("convoy_rides")
            .selectAsFlow(
                ConvoyRide::id,
                filter = FilterOperation("room_id", FilterOperator.EQ, roomId)
            )
            .map { list ->
                list.firstOrNull { it.isActive }
            }
    }

    /**
     * Checks whether the current user has already joined the active ride.
     */
    suspend fun checkHasJoinedRide(rideId: String): Boolean = runCatching {
        val uid = currentUserId ?: return@runCatching false
        val participant = supabase.from("convoy_participants_location")
            .select {
                filter {
                    eq("ride_id", rideId)
                    eq("user_id", uid)
                    eq("has_joined", true)
                }
            }
            .decodeSingleOrNull<ConvoyRiderLocation>()

        participant != null
    }.getOrDefault(false)

    /**
     * Creates a new ride event inside this convoy chat and returns the active ride object.
     */
    suspend fun createConvoyRide(
        roomId: String,
        destinationName: String,
        destinationLat: Double,
        destinationLng: Double
    ): Result<ConvoyRide> = runCatching {
        val uid = currentUserId ?: error("User not authenticated")
        val generatedId = UUID.randomUUID().toString()

        val payload = CreateConvoyRideDto(
            id = generatedId,
            roomId = roomId,
            creatorId = uid,
            destinationName = destinationName.trim(),
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            isActive = true
        )
        supabase.from("convoy_rides").insert(payload)

        // Return immediately for instant local UI update
        ConvoyRide(
            id = generatedId,
            roomId = roomId,
            creatorId = uid,
            destinationName = destinationName.trim(),
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            isActive = true
        )
    }

    /**
     * Deactivates the convoy ride for all members.
     */
    suspend fun endConvoyRide(rideId: String): Result<Unit> = runCatching {
        supabase.from("convoy_rides").update(UpdateConvoyRideStatusDto(isActive = false)) {
            filter {
                eq("id", rideId)
            }
        }
    }

    /**
     * Records that the current rider has agreed to join the ride and share location.
     */
    suspend fun joinConvoyRide(rideId: String): Result<Unit> = runCatching {
        val uid = currentUserId ?: error("User not authenticated")
        val payload = UpsertConvoyRiderLocationDto(
            rideId = rideId,
            userId = uid,
            hasJoined = true,
            updatedAt = getIsoUtcTimestamp()
        )
        supabase.from("convoy_participants_location").upsert(payload)
    }

    /**
     * Leaves the ride and ceases location sharing.
     */
    suspend fun leaveConvoyRide(rideId: String): Result<Unit> = runCatching {
        val uid = currentUserId ?: error("User not authenticated")
        val payload = UpsertConvoyRiderLocationDto(
            rideId = rideId,
            userId = uid,
            hasJoined = false,
            latitude = null,
            longitude = null,
            updatedAt = getIsoUtcTimestamp()
        )
        supabase.from("convoy_participants_location").upsert(payload)
    }

    /**
     * Periodically called by the GPS service to push latest coordinates.
     */
    suspend fun updateRiderLocation(
        rideId: String,
        lat: Double,
        lng: Double,
        heading: Float = 0f,
        speedKmh: Float = 0f
    ): Result<Unit> = runCatching {
        val uid = currentUserId ?: error("User not authenticated")
        val payload = UpsertConvoyRiderLocationDto(
            rideId = rideId,
            userId = uid,
            hasJoined = true,
            latitude = lat,
            longitude = lng,
            heading = heading,
            speedKmh = speedKmh,
            updatedAt = getIsoUtcTimestamp()
        )
        supabase.from("convoy_participants_location").upsert(payload)
    }

    /**
     * Streams riders who have actively joined the ride session.
     */
    @OptIn(SupabaseExperimental::class)
    fun getJoinedRidersStream(rideId: String): Flow<List<ConvoyRiderLocation>> {
        return supabase.from("convoy_participants_location")
            .selectAsFlow(
                ConvoyRiderLocation::userId,
                filter = FilterOperation("ride_id", FilterOperator.EQ, rideId)
            )
            .map { list ->
                list.filter { it.hasJoined }
            }
    }
}