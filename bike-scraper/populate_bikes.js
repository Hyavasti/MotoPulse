import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = 'https://uinwxdnhgzqxwvvibzur.supabase.co';
// Your key works now that the UPDATE policy is active
const SUPABASE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVpbnd4ZG5oZ3pxeHd2dmlienVyIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc5MTQ5ODczNywiZXhwIjoyMTA3MDc0NzM3fQ.4hSQ-tUD1yInyCHRlNfRuHJ3aixb00COzFi9g1DA9eI';

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function fetchBikeImage(brand, modelName) {
  // Regional naming aliases for Wikimedia Commons
  let localizedQuery = `${brand} ${modelName}`;
  if (/click/i.test(modelName)) {
    localizedQuery = `${brand} ${modelName} Vario`;
  } else if (/sniper/i.test(modelName)) {
    localizedQuery = `${brand} ${modelName} Exciter T150`;
  }

  // Extract model displacement (e.g. 125, 150, 160)
  const displacementMatch = modelName.match(/\b\d{3}\b/);
  const targetDisplacement = displacementMatch ? displacementMatch[0] : null;

  const queries = [
    `${localizedQuery} motorcycle`,
    `${brand} ${modelName}`,
    modelName
  ];

  for (const query of queries) {
    try {
      // Searches Wikimedia Commons specifically for actual matching image files
      const commonsUrl = `https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search&gsrnamespace=6&gsrlimit=8&prop=imageinfo&iiprop=url&gsrsearch=${encodeURIComponent(query)}`;
      const res = await fetch(commonsUrl, {
        headers: { 'User-Agent': 'MotorAppScraper/1.0 (contact: student@sti.edu)' }
      });
      const data = await res.json();

      if (data?.query?.pages) {
        const pages = Object.values(data.query.pages);

        // Priority 1: Pick an image matching the specific cc displacement (e.g., 125 vs 150 vs 160)
        let matched = null;
        if (targetDisplacement) {
          matched = pages.find(p => p.title && p.title.includes(targetDisplacement) && !p.title.endsWith('.svg'));
        }

        // Priority 2: Pick the first valid image file
        if (!matched) {
          matched = pages.find(p => p.title && !p.title.endsWith('.svg'));
        }

        const resolvedUrl = matched?.imageinfo?.[0]?.url;
        if (resolvedUrl) {
          return resolvedUrl;
        }
      }
    } catch (_) {}
  }

  // Fallback if no specific photo exists
  return 'https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=600';
}

async function run() {
  console.log('Fetching motorcycles from motorcycle_catalog...');

  const { data: bikes, error } = await supabase
    .from('motorcycle_catalog')
    .select('id, brand, model_name, image_url');

  if (error) {
    console.error('Error fetching bikes:', error);
    return;
  }

  console.log(`Found ${bikes.length} bikes. Updating image links...\n`);

  let count = 0;
  for (const bike of bikes) {
    count++;

    console.log(`[${count}/${bikes.length}] Fetching accurate image for: ${bike.brand} ${bike.model_name}...`);

    const imageUrl = await fetchBikeImage(bike.brand, bike.model_name);

    if (imageUrl) {
      const jsonValue = {
        default: imageUrl
      };

      const { error: updateError } = await supabase
        .from('motorcycle_catalog')
        .update({ image_url: jsonValue })
        .eq('id', bike.id);

      if (updateError) {
        console.error(`  ❌ Failed to update ${bike.model_name}:`, updateError.message);
      } else {
        console.log(`  ✅ Saved: ${imageUrl}`);
      }
    }

    await sleep(350);
  }

  console.log('\n🎉 Finished updating all motorcycle images!');
}

run();