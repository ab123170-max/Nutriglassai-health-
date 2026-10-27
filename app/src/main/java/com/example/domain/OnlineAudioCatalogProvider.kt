package com.example.domain

import com.example.data.model.AudioItemEntity

object OnlineAudioCatalogProvider {

    val featuredPublicDomainCatalog = listOf(
        // Audiobooks
        AudioItemEntity(
            uriString = "https://ia800201.us.archive.org/12/items/art_of_war_librivox/artofwar_01_suntzu_64kb.mp3",
            title = "The Art of War (Chapter 1-3)",
            artistOrAuthor = "Sun Tzu (LibriVox Public Domain)",
            category = "Audiobooks",
            durationMs = 900000L, // 15 mins
            isOnline = true,
            sourceLicenseInfo = "Public Domain (LibriVox Archive)"
        ),
        AudioItemEntity(
            uriString = "https://ia800302.us.archive.org/11/items/meditations_librivox/meditations_01_aurelius_64kb.mp3",
            title = "Meditations (Book 1)",
            artistOrAuthor = "Marcus Aurelius (LibriVox Public Domain)",
            category = "Audiobooks",
            durationMs = 1200000L, // 20 mins
            isOnline = true,
            sourceLicenseInfo = "Public Domain (LibriVox Archive)"
        ),
        AudioItemEntity(
            uriString = "https://ia800708.us.archive.org/18/items/alices_adventures_librivox/alice_01_carroll_64kb.mp3",
            title = "Alice's Adventures in Wonderland (Ch 1)",
            artistOrAuthor = "Lewis Carroll (LibriVox Public Domain)",
            category = "Audiobooks",
            durationMs = 840000L, // 14 mins
            isOnline = true,
            sourceLicenseInfo = "Public Domain (LibriVox Archive)"
        ),

        // Music & Cadence
        AudioItemEntity(
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            title = "120 BPM Energetic Stride Cadence",
            artistOrAuthor = "Open Fitness Audio",
            category = "Music",
            durationMs = 372000L, // ~6 mins
            isOnline = true,
            sourceLicenseInfo = "Creative Commons Open Audio"
        ),
        AudioItemEntity(
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            title = "Himalayan Ambient High-Elevation Stride",
            artistOrAuthor = "Himalayan Wellness Soundscape",
            category = "Music",
            durationMs = 420000L, // 7 mins
            isOnline = true,
            sourceLicenseInfo = "Open License"
        ),

        // Speeches & Podcasts
        AudioItemEntity(
            uriString = "https://ia800301.us.archive.org/28/items/GettysburgAddressLincoln/gettysburg_address_lincoln_64kb.mp3",
            title = "The Gettysburg Address",
            artistOrAuthor = "Abraham Lincoln (Historic Speech)",
            category = "Speeches & Podcasts",
            durationMs = 180000L, // 3 mins
            isOnline = true,
            sourceLicenseInfo = "Public Domain Educational Speech"
        ),

        // Guided Walking
        AudioItemEntity(
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            title = "15-Minute Power Walk & Breathing Guidance",
            artistOrAuthor = "NutriGlass Fitness Coach",
            category = "Guided Walking",
            durationMs = 900000L, // 15 mins
            isOnline = true,
            sourceLicenseInfo = "NutriGlass Certified Guided Track"
        )
    )
}
