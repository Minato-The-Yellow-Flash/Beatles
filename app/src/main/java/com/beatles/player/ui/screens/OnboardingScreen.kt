package com.beatles.player.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.beatles.player.ui.glassRefraction
import com.beatles.player.ui.motion.liquidPress
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beatles.player.R
import com.beatles.player.data.MusicRepository
import com.beatles.player.ui.components.edgeFade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnboardArtist(
    val name: String,
    val imageUrl: String,
    val genre: String,
    val language: String = "All"
)

val INSTANT_RELATED_ARTISTS: Map<String, List<OnboardArtist>> = mapOf(
    "Karan Aujla" to listOf(
        OnboardArtist("Shubh", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("AP Dhillon", "https://cdn-images.dzcdn.net/images/artist/52594ac9fa763dc163ed13d21cb130ec/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Sidhu Moose Wala", "https://cdn-images.dzcdn.net/images/artist/f559ebe3851db26a6a47a76b1d95748f/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Diljit Dosanjh", "https://cdn-images.dzcdn.net/images/artist/79b85e695e0ca6529e56bf3b628e92bd/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Prem Dhillon", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/91/97/81/9197814b-f29e-22e3-6a54-3e9a4f6d4d12/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Arjan Dhillon", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/28/70/4c/28704c7b-3d96-7489-f538-6b8015560e90/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Jordan Sandhu", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7e/3c/65/7e3c65c2-f1bb-8302-3f19-35a11c125633/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Amrit Maan", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/33/c2/f5/33c2f534-192a-fa03-d6c5-59b578c77aa7/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Jerry", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/15/8e/ec/158eec13-e29f-3e8a-0e9f-9e7ec788647a/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Gurinder Gill", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/dc/48/43/dc4843b0-6819-7d88-7243-d933390c2394/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi")
    ),
    "Arijit Singh" to listOf(
        OnboardArtist("Atif Aslam", "https://cdn-images.dzcdn.net/images/artist/0ea90444148fff9c11d77f06a344724e/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
        OnboardArtist("Shreya Ghoshal", "https://cdn-images.dzcdn.net/images/artist/3bb832d37d10ff2affcfa9afdc7c68a0/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
        OnboardArtist("Pritam", "https://cdn-images.dzcdn.net/images/artist/d4914ccd414067cd5e2c108867079a85/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
        OnboardArtist("Mohit Chauhan", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/bf/25/74/bf2574e4-78ff-f1dc-ee2f-933cfb5774a9/cover.jpg/500x500bb.jpg", "Bollywood", "Hindi"),
        OnboardArtist("Jubin Nautiyal", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/44/b3/b2/44b3b28b-b8ba-b485-8025-a745814e49ae/cover.jpg/500x500bb.jpg", "Bollywood", "Hindi"),
        OnboardArtist("Armaan Malik", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/d5/d3/18/d5d31835-ae23-1d90-3490-671c26b52774/cover.jpg/500x500bb.jpg", "Bollywood", "Hindi"),
        OnboardArtist("Darshan Raval", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/1b/02/b7/1b02b78b-dc54-734f-0152-475ea751d382/cover.jpg/500x500bb.jpg", "Indie Pop", "Hindi"),
        OnboardArtist("Vishal Mishra", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/fa/bf/f0/fabff00d-5c02-7632-d81b-513e9a78a6ff/cover.jpg/500x500bb.jpg", "Bollywood", "Hindi"),
        OnboardArtist("KK", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/6b/c4/f2/6bc4f2ec-553b-e854-ce1f-9da1c888d1ee/cover.jpg/500x500bb.jpg", "Bollywood", "Hindi"),
        OnboardArtist("Sonu Nigam", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/8901112440122.jpg/1000x1000bb.jpg", "Indipop Melody", "Hindi")
    ),
    "The Weeknd" to listOf(
        OnboardArtist("Drake", "https://cdn-images.dzcdn.net/images/artist/1051e7fd110f9d3e5e88cdc69c5f227b/500x500-000000-80-0-0.jpg", "Hip-Hop", "English"),
        OnboardArtist("Travis Scott", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/196871341899.jpg/1000x1000bb.jpg", "Hip-Hop", "English"),
        OnboardArtist("Post Malone", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/2e/0f/47/2e0f47e2-7679-b1d5-b91c-77d33d996919/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Bruno Mars", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/33/be/d5/33bed5ea-6536-1e64-42f8-028f09923366/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Justin Bieber", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/66/8f/6f/668f6f59-fcbc-bbff-44e2-637a7feea303/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Kendrick Lamar", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/2f/55/93/2f559388-7517-3801-b3b3-8ec9a5cf6e08/cover.jpg/500x500bb.jpg", "Hip-Hop", "English"),
        OnboardArtist("Frank Ocean", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/95/92/ff/9592ffa2-9e90-c651-789a-0740927da8fa/cover.jpg/500x500bb.jpg", "R&B", "English"),
        OnboardArtist("SZA", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/d9/b4/4d/d9b44d2d-20d0-aa5b-01c5-cb7852cb7ec5/cover.jpg/500x500bb.jpg", "R&B", "English"),
        OnboardArtist("Dua Lipa", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/e5/26/51/e5265147-1f06-2581-c7c4-c8c8868019aa/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Khalid", "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/21/93/ea/2193eab0-c9a1-cb35-ea81-8b0108398188/cover.jpg/500x500bb.jpg", "R&B", "English")
    ),
    "Taylor Swift" to listOf(
        OnboardArtist("Billie Eilish", "https://cdn-images.dzcdn.net/images/artist/8eab1a9a644889aabaca1e193e05f984/500x500-000000-80-0-0.jpg", "Pop", "English"),
        OnboardArtist("Olivia Rodrigo", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/31/53/7c/31537c35-ae52-c3ff-e11a-1d5d27572d42/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Sabrina Carpenter", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/0d/18/eb/0d18eb85-8f6b-8d83-4a16-24ebba054a3a/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Ariana Grande", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/4e/44/21/4e4421b4-b816-6c84-1e0e-9c0d381676d1/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Selena Gomez", "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/93/ea/cb/93eacb0d-b778-f739-2b02-5e9bc669fcf3/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Lana Del Rey", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/09/a3/ae/09a3aefb-dbd9-81a6-5749-8c08dc42f36f/cover.jpg/500x500bb.jpg", "Indie Pop", "English"),
        OnboardArtist("Katy Perry", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b8/ec/a9/b8eca9c8-04ba-81c8-2b8d-d790407137f8/cover.jpg/500x500bb.jpg", "Pop", "English"),
        OnboardArtist("Gracie Abrams", "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/37/10/a5/3710a562-b7e4-2396-d8bb-e0573e0a0d54/cover.jpg/500x500bb.jpg", "Indie Pop", "English"),
        OnboardArtist("Ed Sheeran", "https://cdn-images.dzcdn.net/images/artist/d6bb84390641d8ae9118228d9544e53d/500x500-000000-80-0-0.jpg", "Pop", "English"),
        OnboardArtist("Shawn Mendes", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/8e/3c/66/8e3c6676-e0e5-79b8-3e41-0f7362a3ea42/cover.jpg/500x500bb.jpg", "Pop", "English")
    ),
    "Diljit Dosanjh" to listOf(
        OnboardArtist("Karan Aujla", "https://cdn-images.dzcdn.net/images/artist/2342131131749544b2563ecad0068eef/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Amrinder Gill", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/8a/95/96/8a959663-883a-d68a-6ce4-ea04bc999818/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Gippy Grewal", "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/64/09/a1/6409a1f2-1a41-267f-445a-c5ee9659b9aa/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
        OnboardArtist("Guru Randhawa", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/10/4e/d4/104ed4e3-3631-cbb0-2646-9769db0e7e17/cover.jpg/500x500bb.jpg", "Punjabi Pop", "Punjabi"),
        OnboardArtist("B Praak", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/b9/eb/39/b9eb399a-f4ef-9b65-680f-90e633d7b822/cover.jpg/500x500bb.jpg", "Punjabi Melodies", "Punjabi"),
        OnboardArtist("Jassie Gill", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/f6/cb/3d/f6cb3d4b-7489-cf77-d4fa-a5e2f7b4ce25/cover.jpg/500x500bb.jpg", "Punjabi Pop", "Punjabi"),
        OnboardArtist("Harrdy Sandhu", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/fc/c4/81/fcc48187-54b9-8bf9-2187-e2c7a31ebf10/cover.jpg/500x500bb.jpg", "Punjabi Pop", "Punjabi"),
        OnboardArtist("Sunanda Sharma", "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/05/23/94/0523945a-37c2-f1d7-2eb1-76634563a3fe/cover.jpg/500x500bb.jpg", "Punjabi Pop", "Punjabi"),
        OnboardArtist("Nimrat Khaira", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/13/2e/51/132e5192-d9c9-5246-8149-14a9386c9d09/cover.jpg/500x500bb.jpg", "Punjabi Folk", "Punjabi"),
        OnboardArtist("Parmish Verma", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/33/c2/f5/33c2f534-192a-fa03-d6c5-59b578c77aa7/cover.jpg/500x500bb.jpg", "Punjabi Pop", "Punjabi")
    ),
    "Badshah" to listOf(
        OnboardArtist("Raftaar", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/13/2e/51/132e5192-d9c9-5246-8149-14a9386c9d09/cover.jpg/1000x1000bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("Yo Yo Honey Singh", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/55/87/44/5587440b-7411-cf06-1509-f642468307d1/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("KR\$NA", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("Ikka", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/bf/25/74/bf2574e4-78ff-f1dc-ee2f-933cfb5774a9/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("DIVINE", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/cover.jpg/500x500bb.jpg", "Gully Rap", "Hindi"),
        OnboardArtist("Seedhe Maut", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("MC Stan", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("EMIWAY BANTAI", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/3d/8b/4f/3d8b4f0d-e215-dc34-934a-9ef8684d0b2f/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("Fotty Seven", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi"),
        OnboardArtist("Paradox", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", "Desi Hip-Hop", "Hindi")
    ),
    "Anirudh Ravichander" to listOf(
        OnboardArtist("Sid Sriram", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/92/ff/fb/92fffb8d-6490-482a-0a79-c8c7f3e8b093/8903431872166_cover.jpg/500x500bb.jpg", "Carnatic Pop", "Tamil"),
        OnboardArtist("A.R. Rahman", "https://cdn-images.dzcdn.net/images/artist/bd34315ef977a62a9e28c1ab19bb8ac4/500x500-000000-80-0-0.jpg", "Kollywood", "Tamil"),
        OnboardArtist("Yuvan Shankar Raja", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/33/be/d5/33bed5ea-6536-1e64-42f8-028f09923366/cover.jpg/500x500bb.jpg", "Kollywood", "Tamil"),
        OnboardArtist("Harris Jayaraj", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/bf/25/74/bf2574e4-78ff-f1dc-ee2f-933cfb5774a9/cover.jpg/500x500bb.jpg", "Kollywood", "Tamil"),
        OnboardArtist("Santhosh Narayanan", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/cover.jpg/500x500bb.jpg", "Kollywood", "Tamil"),
        OnboardArtist("Devi Sri Prasad", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/27/ef/82/27ef822c-a2da-dc80-92a5-4dc9cfda4752/cover.jpg/500x500bb.jpg", "Tollywood", "Telugu"),
        OnboardArtist("Thaman S", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/cover.jpg/500x500bb.jpg", "Tollywood", "Telugu"),
        OnboardArtist("GV Prakash Kumar", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/cover.jpg/500x500bb.jpg", "Kollywood", "Tamil"),
        OnboardArtist("Pradeep Kumar", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/fa/bf/f0/fabff00d-5c02-7632-d81b-513e9a78a6ff/cover.jpg/500x500bb.jpg", "Indie Tamil", "Tamil"),
        OnboardArtist("Sean Roldan", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/33/c2/f5/33c2f534-192a-fa03-d6c5-59b578c77aa7/cover.jpg/500x500bb.jpg", "Indie Tamil", "Tamil")
    ),
    "Pawan Singh" to listOf(
        OnboardArtist("Khesari Lal Yadav", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/9f/bf/02/9fbf02ef-9cb4-8a4c-5a6f-f6f87ea960e6/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Ritesh Pandey", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/21/df/4421df65-8b38-d7e7-a9a5-731ff7ec56bb/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Arvind Akela Kallu", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Neelkamal Singh", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/3d/8b/4f/3d8b4f0d-e215-dc34-934a-9ef8684d0b2f/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Shilpi Raj", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/fa/bf/f0/fabff00d-5c02-7632-d81b-513e9a78a6ff/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Pramod Premi Yadav", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Ankush Raja", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/33/c2/f5/33c2f534-192a-fa03-d6c5-59b578c77aa7/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Gunjan Singh", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Samar Singh", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
        OnboardArtist("Antra Singh Priyanka", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/8a/95/96/8a959663-883a-d68a-6ce4-ea04bc999818/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri")
    ),
    "Renuka Panwar" to listOf(
        OnboardArtist("Gulzaar Chhaniwala", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/3d/8b/4f/3d8b4f0d-e215-dc34-934a-9ef8684d0b2f/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Diler Kharkiya", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/21/df/4421df65-8b38-d7e7-a9a5-731ff7ec56bb/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Masoom Sharma", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/9f/bf/02/9fbf02ef-9cb4-8a4c-5a6f-f6f87ea960e6/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Ajay Hooda", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Raju Punjabi", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("KD Desirock", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/7d/5d/44/7d5d4444-245c-e40f-7649-650058b725b7/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Ruchika Jangid", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/fa/bf/f0/fabff00d-5c02-7632-d81b-513e9a78a6ff/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Amit Saini Rohtakiya", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/33/c2/f5/33c2f534-192a-fa03-d6c5-59b578c77aa7/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Sumit Goswami", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/07/21/df/0721df26-f7be-db50-7dc4-6e06821262d6/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
        OnboardArtist("Bintu Pabra", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/8a/95/96/8a959663-883a-d68a-6ce4-ea04bc999818/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi")
    )
)

val CURATED_ONBOARD_ARTISTS = listOf(
    // Balanced global & diverse artists across all regional and international languages
    OnboardArtist("The Weeknd", "https://cdn-images.dzcdn.net/images/artist/581693b4724a7fcfa754455101e13a44/500x500-000000-80-0-0.jpg", "Pop", "English"),
    OnboardArtist("Arijit Singh", "https://cdn-images.dzcdn.net/images/artist/ac5350cff290edd5b69fa584b8b1bd4f/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
    OnboardArtist("Taylor Swift", "https://cdn-images.dzcdn.net/images/artist/cc2495870fe1a792ad0cdb05501ad5ec/500x500-000000-80-0-0.jpg", "Pop", "English"),
    OnboardArtist("Karan Aujla", "https://cdn-images.dzcdn.net/images/artist/2342131131749544b2563ecad0068eef/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
    OnboardArtist("Billie Eilish", "https://cdn-images.dzcdn.net/images/artist/8eab1a9a644889aabaca1e193e05f984/500x500-000000-80-0-0.jpg", "Pop", "English"),
    OnboardArtist("Shreya Ghoshal", "https://cdn-images.dzcdn.net/images/artist/3bb832d37d10ff2affcfa9afdc7c68a0/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
    OnboardArtist("Diljit Dosanjh", "https://cdn-images.dzcdn.net/images/artist/79b85e695e0ca6529e56bf3b628e92bd/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
    OnboardArtist("Coldplay", "https://cdn-images.dzcdn.net/images/artist/3087954bca22f306324912e5ac8375c3/500x500-000000-80-0-0.jpg", "Pop", "English"),
    OnboardArtist("Atif Aslam", "https://cdn-images.dzcdn.net/images/artist/0ea90444148fff9c11d77f06a344724e/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
    OnboardArtist("Sidhu Moose Wala", "https://cdn-images.dzcdn.net/images/artist/f559ebe3851db26a6a47a76b1d95748f/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
    OnboardArtist("Ed Sheeran", "https://cdn-images.dzcdn.net/images/artist/d6bb84390641d8ae9118228d9544e53d/500x500-000000-80-0-0.jpg", "Pop", "English"),
    OnboardArtist("A.R. Rahman", "https://cdn-images.dzcdn.net/images/artist/bd34315ef977a62a9e28c1ab19bb8ac4/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
    OnboardArtist("Shubh", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", "Punjabi", "Punjabi"),
    OnboardArtist("Drake", "https://cdn-images.dzcdn.net/images/artist/1051e7fd110f9d3e5e88cdc69c5f227b/500x500-000000-80-0-0.jpg", "Hip-Hop", "English"),
    OnboardArtist("Pritam", "https://cdn-images.dzcdn.net/images/artist/d4914ccd414067cd5e2c108867079a85/500x500-000000-80-0-0.jpg", "Bollywood", "Hindi"),
    OnboardArtist("AP Dhillon", "https://cdn-images.dzcdn.net/images/artist/52594ac9fa763dc163ed13d21cb130ec/500x500-000000-80-0-0.jpg", "Punjabi", "Punjabi"),
    OnboardArtist("Badshah", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/58/01/fa/5801fa04-7a91-4470-3444-eaae3b25f549/8903431976079_cover.jpg/1000x1000bb.jpg", "Indian Hip-Hop", "Hindi"),
    OnboardArtist("Sonu Nigam", "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/28/62/1d/28621d9f-a89c-5674-3c6c-a49688a202c4/8901112440122.jpg/1000x1000bb.jpg", "Indipop Melody", "Hindi"),
    OnboardArtist("Raftaar", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/13/2e/51/132e5192-d9c9-5246-8149-14a9386c9d09/cover.jpg/1000x1000bb.jpg", "Desi Hip-Hop", "Hindi"),
    OnboardArtist("Renuka Panwar", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/21/df/4421df65-8b38-d7e7-a9a5-731ff7ec56bb/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
    OnboardArtist("Gulzaar Chhaniwala", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/3d/8b/4f/3d8b4f0d-e215-dc34-934a-9ef8684d0b2f/cover.jpg/500x500bb.jpg", "Haryanvi", "Haryanvi"),
    OnboardArtist("Pawan Singh", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/fa/1c/bf/fa1cbf5c-20ce-e555-520e-6f8c0a8bc02a/8903431782298_cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
    OnboardArtist("Khesari Lal Yadav", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/9f/bf/02/9fbf02ef-9cb4-8a4c-5a6f-f6f87ea960e6/cover.jpg/500x500bb.jpg", "Bhojpuri", "Bhojpuri"),
    OnboardArtist("Anirudh Ravichander", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/21/04/e7/2104e76a-549d-e4fb-79f9-e3954bb04c6f/8903431945198_cover.jpg/500x500bb.jpg", "Kollywood", "Tamil"),
    OnboardArtist("Sid Sriram", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/92/ff/fb/92fffb8d-6490-482a-0a79-c8c7f3e8b093/8903431872166_cover.jpg/500x500bb.jpg", "Carnatic Pop", "Tamil"),
    OnboardArtist("Devi Sri Prasad", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/27/ef/82/27ef822c-a2da-dc80-92a5-4dc9cfda4752/cover.jpg/500x500bb.jpg", "Tollywood", "Telugu"),
    OnboardArtist("Sushin Shyam", "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/e5/26/51/e5265147-1f06-2581-c7c4-c8c8868019aa/cover.jpg/500x500bb.jpg", "Mollywood", "Malayalam"),
    OnboardArtist("Sanjith Hegde", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/b4/0e/0f/b40e0fa7-7e61-a0c5-e51c-ef66524bf51a/cover.jpg/500x500bb.jpg", "Sandalwood", "Kannada"),
    OnboardArtist("Anupam Roy", "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/65/56/6d/65566df9-8488-812e-13c5-f938d6fcbcae/cover.jpg/500x500bb.jpg", "Bengali Folk", "Bengali"),
    OnboardArtist("Ajay-Atul", "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/99/31/58/993158c5-9b7e-e11a-569a-6a5ce8c843bc/cover.jpg/500x500bb.jpg", "Marathi Cinema", "Marathi"),
    OnboardArtist("Aditya Gadhvi", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/fc/c4/81/fcc48187-54b9-8bf9-2187-e2c7a31ebf10/cover.jpg/500x500bb.jpg", "Gujarati Folk", "Gujarati"),
    OnboardArtist("Mame Khan", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/c3/00/62/c30062df-a4bb-3ef7-4a0b-1915ea0aa9c7/886443993212.jpg/1000x1000bb.jpg", "Rajasthani Folk", "Rajasthani"),
    OnboardArtist("Bad Bunny", "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/5a/92/b8/5a92b8d4-5390-3404-e58f-b5dc561ea908/197188730906.jpg/500x500bb.jpg", "Latin Pop", "Spanish"),
    OnboardArtist("BTS", "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/05/23/94/0523945a-37c2-f1d7-2eb1-76634563a3fe/196922060876_Cover.jpg/500x500bb.jpg", "K-Pop", "Korean"),
    OnboardArtist("YOASOBI", "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/c6/16/e0/c616e00b-1793-195c-554d-7467383c27e0/cover.jpg/500x500bb.jpg", "J-Pop", "Japanese"),
    OnboardArtist("Stromae", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/44/26/51/4426514e-4f36-3a7b-3b8c-57cae4db23d9/cover.jpg/500x500bb.jpg", "French Pop", "French"),
    OnboardArtist("Amr Diab", "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/7d/4f/94/7d4f9468-56e1-3a2d-7186-c8088170ef58/196871341899.jpg/1000x1000bb.jpg", "Arabic Pop", "Arabic")
)

val ONBOARD_ARTISTS = CURATED_ONBOARD_ARTISTS.map { it.name to it.imageUrl }

private val GrayBlackBg = Color(0xFF141416)
private val InactiveButtonBg = Color(0xFF262628)
private val ActiveButtonBg = Color(0xFFE2E2E2)
private val AccentRed = Color(0xFFF82C38)

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeScript: String,
    val description: String
)

val MUSIC_LANGUAGES = listOf(
    LanguageOption("Punjabi", "Punjabi", "ਪੰਜਾਬੀ", "Bhangra, Pop & Desi Hits"),
    LanguageOption("Hindi", "Hindi", "हिंदी", "Bollywood, Indie & Melodies"),
    LanguageOption("English", "English", "English", "Global Pop, Hip-Hop & Rock"),
    LanguageOption("Haryanvi", "Haryanvi", "हरियाणवी", "Desi Folk, Ragni & Beats"),
    LanguageOption("Bhojpuri", "Bhojpuri", "भोजपुरी", "Purvanchal Hits & Lokgeet"),
    LanguageOption("Tamil", "Tamil", "தமிழ்", "Kollywood & Carnatic Waves"),
    LanguageOption("Telugu", "Telugu", "తెలుగు", "Tollywood, Mass & Melodies"),
    LanguageOption("Malayalam", "Malayalam", "മലയാളം", "Mollywood & Indie Waves"),
    LanguageOption("Kannada", "Kannada", "ಕನ್ನಡ", "Sandalwood & Retro Hits"),
    LanguageOption("Bengali", "Bengali", "বাংলা", "Rabindra Sangeet & Modern"),
    LanguageOption("Marathi", "Marathi", "मराठी", "Natya Sangeet & Folk Pop"),
    LanguageOption("Gujarati", "Gujarati", "ગુજરાતી", "Garba, Sugam & Folk"),
    LanguageOption("Rajasthani", "Rajasthani", "राजस्थानी", "Marwadi & Folk Fusion"),
    LanguageOption("Spanish", "Spanish", "Español", "Reggaeton, Latin Pop & Salsa"),
    LanguageOption("Korean", "Korean", "한국어", "K-Pop, OST & Indie"),
    LanguageOption("Japanese", "Japanese", "日本語", "J-Pop, Anime & City Pop"),
    LanguageOption("French", "French", "Français", "French Pop & Chanson"),
    LanguageOption("Arabic", "Arabic", "العربية", "Khaleeji & Arabic Pop"),
    LanguageOption("All", "All Languages", "Global", "Mix of everything")
)

@Composable
fun OnboardStep1(onNext: (String) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) GrayBlackBg else Color(0xFFF6F7F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val inputBg = if (isDark) Color(0xFF26262A) else Color(0xFFE5E7EB)
    val btnActiveBg = if (isDark) ActiveButtonBg else Color(0xFF141416)
    val btnActiveText = if (isDark) GrayBlackBg else Color.White
    val btnInactiveBg = if (isDark) InactiveButtonBg else Color(0xFFE5E7EB)
    val btnInactiveText = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)

    var name by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light),
                    contentDescription = "Beatles",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("beatles music", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text("the ocean of music awaits you", fontSize = 14.sp, color = textSecondary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("What should we call you?", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(color = textPrimary, fontSize = 16.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(if (isDark) Color.White else AccentRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0xFF1E2024).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f))
                        .glassRefraction(50.dp, isDark = isDark)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text("Your name...", color = textSecondary, fontSize = 16.sp)
                        inner()
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val nextInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp, top = 8.dp)
                    .height(56.dp)
                    .liquidPress(nextInteraction)
                    .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f))
                    .clip(RoundedCornerShape(50))
                    .background(if (name.isNotBlank()) btnActiveBg else btnInactiveBg)
                    .glassRefraction(50.dp, isDark = isDark)
                    .clickable(enabled = name.isNotBlank(), interactionSource = nextInteraction, indication = null) {
                        onNext(name.trim())
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Next",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (name.isNotBlank()) btnActiveText else btnInactiveText
                )
            }
        }
    }
}

/**
 * Step 2: Dedicated Multi-Select Music Language Selection.
 * Allows user to pick multiple music languages (regional & international).
 */
@Composable
fun OnboardStepLanguage(
    initialLanguages: Set<String> = emptySet(),
    onBack: () -> Unit = {},
    onNext: (Set<String>) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) GrayBlackBg else Color(0xFFF6F7F9)
    val cardBg = if (isDark) Color(0xFF1E2024) else Color.White
    val cardSelectedBg = if (isDark) Color(0xFF2A2A32) else Color(0xFFFFECEE)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val btnActiveBg = if (isDark) ActiveButtonBg else Color(0xFF141416)
    val btnActiveText = if (isDark) GrayBlackBg else Color.White
    val btnInactiveBg = if (isDark) InactiveButtonBg else Color(0xFFE5E7EB)
    val btnInactiveText = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)

    var selectedLangs by remember {
        mutableStateOf(if (initialLanguages.isNotEmpty()) initialLanguages else setOf("Punjabi", "Hindi"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 35f) {
                        change.consume()
                        onBack()
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 48.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 12.dp)) {
                Text("Choose your\nmusic languages", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textPrimary, lineHeight = 34.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Select all regional and international languages you love listening to.", fontSize = 14.sp, color = textSecondary)
            }

            // Language Cards Grid
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .edgeFade(bg),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(MUSIC_LANGUAGES) { lang ->
                        val isSelected = selectedLangs.contains(lang.code)
                        val cardInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .height(125.dp)
                                .liquidPress(cardInteraction)
                                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) cardSelectedBg else cardBg)
                                .glassRefraction(20.dp, isDark = isDark)
                                .clickable(interactionSource = cardInteraction, indication = null) {
                                    selectedLangs = if (lang.code == "All") {
                                        if (isSelected) emptySet() else setOf("All")
                                    } else {
                                        val base = selectedLangs - "All"
                                        if (isSelected) base - lang.code else base + lang.code
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        lang.nativeScript,
                                        color = if (isSelected) AccentRed else textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(AccentRed, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    lang.name,
                                    color = if (isSelected && !isDark) AccentRed else textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    lang.description,
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Bottom CTA
            val langNextInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp, top = 8.dp)
                    .height(56.dp)
                    .liquidPress(langNextInteraction)
                    .shadow(3.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f))
                    .clip(RoundedCornerShape(50))
                    .background(if (selectedLangs.isNotEmpty()) btnActiveBg else btnInactiveBg)
                    .glassRefraction(50.dp, isDark = isDark)
                    .clickable(enabled = selectedLangs.isNotEmpty(), interactionSource = langNextInteraction, indication = null) {
                        onNext(selectedLangs)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (selectedLangs.size > 1) "Next (${selectedLangs.size} languages selected)" else if (selectedLangs.size == 1) "Next · ${selectedLangs.first()}" else "Next",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedLangs.isNotEmpty()) btnActiveText else btnInactiveText
                )
            }
        }
    }
}

/**
 * Step 3: Dedicated Favorite Artists Selection.
 * Shows curated artists filtered by the user's selected language, with search, language filter chips, and dynamic suggestions.
 */
@Composable
fun OnboardStepArtists(
    selectedLanguage: String = "Punjabi",
    onBack: () -> Unit = {},
    onNext: (Set<String>) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) GrayBlackBg else Color(0xFFF6F7F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val chipBg = if (isDark) Color(0xFF222226) else Color.White
    val chipBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    val searchBg = if (isDark) Color(0xFF222226) else Color.White
    val searchBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val cardBg = if (isDark) Color(0xFF202024) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val btnActiveBg = if (isDark) ActiveButtonBg else Color(0xFF141416)
    val btnActiveText = if (isDark) GrayBlackBg else Color.White
    val btnInactiveBg = if (isDark) InactiveButtonBg else Color(0xFFE5E7EB)
    val btnInactiveText = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF9CA3AF)

    var activeLanguage by remember { mutableStateOf(selectedLanguage) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var dynamicSuggestions by remember { mutableStateOf(listOf<OnboardArtist>()) }
    var fetchedFor by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    // Dynamically recommend similar artists whenever selection changes
    LaunchedEffect(selected) {
        val newSelections = selected - fetchedFor
        for (artistName in newSelections) {
            scope.launch {
                val suggested = MusicRepository.getSuggestedArtists(artistName)
                val existingNames = (CURATED_ONBOARD_ARTISTS.map { it.name } + dynamicSuggestions.map { it.name }).toSet()
                val newArtists = suggested
                    .filter { it.first !in existingNames && it.first !in selected }
                    .map { OnboardArtist(it.first, it.second, "Recommended", activeLanguage) }
                if (newArtists.isNotEmpty()) {
                    dynamicSuggestions = (dynamicSuggestions + newArtists).distinctBy { it.name }
                }
            }
        }
        fetchedFor = selected
    }

    val allArtists = (CURATED_ONBOARD_ARTISTS + dynamicSuggestions).distinctBy { it.name }
    val filteredArtists = allArtists.filter { artist ->
        val matchesLang = activeLanguage == "All" ||
                artist.language.equals(activeLanguage, ignoreCase = true) ||
                artist.genre.contains(activeLanguage, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || artist.name.contains(searchQuery.trim(), ignoreCase = true)
        matchesLang && matchesSearch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 35f) {
                        change.consume()
                        onBack()
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 48.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 8.dp)) {
                Text(
                    "Tell us about your\nfav artists",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    lineHeight = 32.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Select your favorite $activeLanguage artists to craft your feed",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            // 1. Inline Search Bar (At the top of the selection section)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f))
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color(0xFF1E2024).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f))
                    .glassRefraction(50.dp, isDark = isDark)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            if (q.length > 2 && allArtists.none { it.name.contains(q, ignoreCase = true) }) {
                                scope.launch {
                                    val suggested = MusicRepository.getSuggestedArtists(q)
                                    val newArtists = suggested.map { OnboardArtist(it.first, it.second, "Custom", activeLanguage) }
                                    dynamicSuggestions = (dynamicSuggestions + newArtists).distinctBy { it.name }
                                }
                            }
                        },
                        textStyle = TextStyle(color = textPrimary, fontSize = 14.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isDark) Color.White else AccentRed),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("Search more artists...", color = textSecondary, fontSize = 13.sp)
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Comfortable spacing between search bar and language filter chips bracket
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Language Filter Chips Bracket: Moved lower from the search bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(MUSIC_LANGUAGES) { lang ->
                    val isSelected = activeLanguage.equals(lang.code, ignoreCase = true)
                    val chipInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .liquidPress(chipInteraction)
                            .shadow(2.dp, RoundedCornerShape(50), spotColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.10f))
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) AccentRed
                                else if (isDark) Color(0xFF1E2024).copy(alpha = 0.70f)
                                else Color(0xFFE2E2E2).copy(alpha = 0.60f)
                            )
                            .glassRefraction(50.dp, isDark = isDark)
                            .clickable(interactionSource = chipInteraction, indication = null) { activeLanguage = lang.code }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = lang.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Artists Grid
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .edgeFade(bg, top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
                ) {
                    items(filteredArtists) { artist ->
                        val isSelected = selected.contains(artist.name)
                        Box(
                            modifier = Modifier
                                .height(190.dp)
                                .shadow(3.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .glassRefraction(20.dp, isDark = isDark)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AccentRed else cardBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .liquidPress(enabled = true)
                                .clickable {
                                    val willBeSelected = !isSelected
                                    selected = if (isSelected) selected - artist.name else selected + artist.name
                                    // ⚡ Instantaneous suggestions (0ms response)
                                    if (willBeSelected) {
                                        val instantRecs = INSTANT_RELATED_ARTISTS[artist.name] ?: emptyList()
                                        if (instantRecs.isNotEmpty()) {
                                            val existingNames = (CURATED_ONBOARD_ARTISTS.map { it.name } + dynamicSuggestions.map { it.name }).toSet()
                                            val newRecs = instantRecs.filter { it.name !in existingNames && it.name !in selected }
                                            if (newRecs.isNotEmpty()) {
                                                dynamicSuggestions = (dynamicSuggestions + newRecs).distinctBy { it.name }
                                            }
                                        }
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = artist.imageUrl,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                        .size(26.dp)
                                        .background(AccentRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    artist.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    artist.genre,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom CTA
            Button(
                onClick = { onNext(selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp, top = 8.dp)
                    .height(56.dp)
                    .shadow(if (selected.isNotEmpty()) 4.dp else 0.dp, RoundedCornerShape(50))
                    .glassRefraction(50.dp, isDark = isDark)
                    .liquidPress(enabled = selected.isNotEmpty()),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnActiveBg,
                    disabledContainerColor = btnInactiveBg
                )
            ) {
                Text(
                    if (selected.isNotEmpty()) "Next (${selected.size} selected)" else "Select at least 1",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected.isNotEmpty()) btnActiveText else btnInactiveText
                )
            }
        }
    }
}

/**
 * Step 4: Crafting the perfect playlist / Hop-in progress screen.
 */
@Composable
fun OnboardStepReady(name: String, hasData: Boolean = false, onPrepareData: () -> Unit = {}, onDone: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) GrayBlackBg else Color(0xFFF6F7F9)
    val textPrimary = if (isDark) Color.White else Color(0xFF141416)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val logoBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val trackBg = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)

    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        label = "progressAnim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )

    // Determinate progress counting 0% to 100% over ~2.2 seconds, pre-warming all personalized data during transition
    LaunchedEffect(Unit) {
        onPrepareData()
        for (i in 1..100) {
            delay(22)
            progress = i / 100f
        }
        delay(250)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .shadow(4.dp, CircleShape)
                    .glassRefraction(45.dp, isDark = isDark, isCircle = true)
                    .background(logoBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = if (isDark) R.drawable.beatles_logo else R.drawable.beatles_logo_light),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                if (name.isNotEmpty()) "Crafting the perfect playlist for $name" else "Crafting the perfect playlist",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Personalizing your music experience...", fontSize = 15.sp, color = textSecondary)
            Spacer(modifier = Modifier.height(32.dp))

            // Determinate Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentRed,
                trackColor = trackBg
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Live Percentage Indicator
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// Backward-compatible aliases
@Composable
fun OnboardStep2(initialLanguage: String = "Punjabi", onNext: (Set<String>, String) -> Unit) {
    OnboardStepLanguage(initialLanguages = setOf(initialLanguage), onNext = { langs -> onNext(langs, langs.firstOrNull() ?: "All") })
}

@Composable
fun OnboardStep3(name: String, hasData: Boolean = false, onDone: () -> Unit) {
    OnboardStepReady(name = name, hasData = hasData, onDone = onDone)
}
