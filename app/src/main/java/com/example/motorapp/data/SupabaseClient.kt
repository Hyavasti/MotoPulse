package com.example.motorapp.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

val supabase = createSupabaseClient(
    supabaseUrl = "https://uinwxdnhgzqxwvvibzur.supabase.co",
    supabaseKey = "sb_publishable_FvEb8OJ7uj8vEKgTBiXAAA_t-jGjC-x"
) {
    install(Auth)
    install(Postgrest) {
        serializer = KotlinXSerializer(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        )
    }

    install(Storage)
    install(Realtime)
}