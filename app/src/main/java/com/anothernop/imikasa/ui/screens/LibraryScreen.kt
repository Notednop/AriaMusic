package com.anothernop.imikasa.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.anothernop.imikasa.R
import com.anothernop.imikasa.audio.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    trackList: List<Track>,
    currentTrack: Track?,
    onTrackSelect: (Track) -> Unit,
    onRefreshRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Choose correct permission string based on OS version
    val permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionString) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            onRefreshRequest()
        }
    }

    // Category tracking state
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Artist", "Album", "Single", "Folder")

    // Filter states
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    // Reset drill-down selection when category changes
    LaunchedEffect(selectedCategory) {
        selectedArtist = null
        selectedAlbum = null
        selectedFolder = null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(horizontal = 16.dp)
            .testTag("library_screen")
    ) {
        // Library Header Title
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "YOUR LOCAL AND SYNTHESIZED TRACKS",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE23E57),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                IconButton(
                    onClick = {
                        if (hasPermission) {
                            onRefreshRequest()
                        } else {
                            launcher.launch(permissionString)
                        }
                    },
                    modifier = Modifier.background(Color(0xFF121212), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Media",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Display Category chip selector row below header
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(text = category, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF121212),
                            selectedContainerColor = Color(0xFFE23E57),
                            labelColor = Color.Gray,
                            selectedLabelColor = Color.White
                        ),
                        border = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Display Permission Card if storage permission is not granted
        if (!hasPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("permission_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Library Music Icon",
                            tint = Color(0xFFE23E57),
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Import Local Device Music",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "To scan and play your personal music library offline, iMikasa needs access to your device storage files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { launcher.launch(permissionString) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE23E57)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("request_permission_button")
                        ) {
                            Text("Grant Storage Access", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Library Listing Section
        if (trackList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No offline audio files found.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            when (selectedCategory) {
                "All" -> {
                    // Full list
                    items(trackList, key = { it.id }) { track ->
                        LibraryTrackRow(track, currentTrack, onTrackSelect)
                    }
                }
                "Artist" -> {
                    if (selectedArtist == null) {
                        // Display list of unique artists
                        val artists = trackList.map { it.artist }.distinct().sorted()
                        items(artists) { artistName ->
                            val artistTracksCount = trackList.count { it.artist == artistName }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedArtist = artistName }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Artist",
                                    tint = Color(0xFFE23E57),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = artistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "$artistTracksCount tracks", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF121212), thickness = 0.8.dp)
                        }
                    } else {
                        // Display back button and tracks for selected artist
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedArtist = null }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFE23E57)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Artists / $selectedArtist",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        val artistTracks = trackList.filter { it.artist == selectedArtist }
                        items(artistTracks, key = { it.id }) { track ->
                            LibraryTrackRow(track, currentTrack, onTrackSelect)
                        }
                    }
                }
                "Album" -> {
                    if (selectedAlbum == null) {
                        // Display list of unique albums
                        val albums = trackList.map { it.album }.distinct().sorted()
                        items(albums) { albumName ->
                            val albumTracksCount = trackList.count { it.album == albumName }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAlbum = albumName }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = "Album",
                                    tint = Color(0xFFE23E57),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = albumName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "$albumTracksCount tracks", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF121212), thickness = 0.8.dp)
                        }
                    } else {
                        // Display back button and tracks for selected album
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAlbum = null }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFE23E57)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Albums / $selectedAlbum",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        val albumTracks = trackList.filter { it.album == selectedAlbum }
                        items(albumTracks, key = { it.id }) { track ->
                            LibraryTrackRow(track, currentTrack, onTrackSelect)
                        }
                    }
                }
                "Single" -> {
                    // Display only tracks that are singles (Standalone tracks)
                    // Standalone tracks usually have "Offline Library", "Unknown Album", or matches Single keywords
                    val singles = trackList.filter {
                        it.album.equals("Offline Library", true) ||
                        it.album.equals("Unknown Album", true) ||
                        it.album.contains("single", true) ||
                        it.album.isBlank()
                    }
                    if (singles.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "No standalone singles found.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(singles, key = { it.id }) { track ->
                            LibraryTrackRow(track, currentTrack, onTrackSelect)
                        }
                    }
                }
                "Folder" -> {
                    if (selectedFolder == null) {
                        // Group tracks by their folder directory
                        val folders = trackList.map { track ->
                            val path = track.filePath
                            if (path != null) {
                                val file = java.io.File(path)
                                file.parent ?: "Internal Storage"
                            } else {
                                "iMikasa Synthesizer Assets"
                            }
                        }.distinct().sorted()

                        items(folders) { folderPath ->
                            val folderTracksCount = trackList.count { track ->
                                val path = track.filePath
                                if (path != null) {
                                    val file = java.io.File(path)
                                    (file.parent ?: "Internal Storage") == folderPath
                                } else {
                                    folderPath == "iMikasa Synthesizer Assets"
                                }
                            }
                            val displayName = folderPath.substringAfterLast("/")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolder = folderPath }
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = Color(0xFFE23E57),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = folderPath, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "$folderTracksCount tracks", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF121212), thickness = 0.8.dp)
                        }
                    } else {
                        // Display back button and tracks for selected folder
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFolder = null }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFE23E57)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Folders / ${selectedFolder?.substringAfterLast("/")}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        val folderTracks = trackList.filter { track ->
                            val path = track.filePath
                            if (path != null) {
                                val file = java.io.File(path)
                                (file.parent ?: "Internal Storage") == selectedFolder
                            } else {
                                selectedFolder == "iMikasa Synthesizer Assets"
                            }
                        }
                        items(folderTracks, key = { it.id }) { track ->
                            LibraryTrackRow(track, currentTrack, onTrackSelect)
                        }
                    }
                }
            }
        }

        // Offset bottom for miniplayer spacing
        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun LibraryTrackRow(
    track: Track,
    currentTrack: Track?,
    onTrackSelect: (Track) -> Unit
) {
    val context = LocalContext.current
    val isPlayingThis = currentTrack?.id == track.id

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTrackSelect(track) }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("library_track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Asynchronous, on-demand local album art loading with zero memory footprint leaks!
        var libEmbeddedImage by remember(track.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

        LaunchedEffect(track.id) {
            if (track.embeddedArt != null) {
                try {
                    val bytes = track.embeddedArt
                    libEmbeddedImage = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } catch (e: Exception) {}
            } else {
                withContext(Dispatchers.IO) {
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        if (track.filePath != null && java.io.File(track.filePath).exists()) {
                            retriever.setDataSource(track.filePath)
                            val bytes = retriever.embeddedPicture
                            if (bytes != null) {
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    withContext(Dispatchers.Main) {
                                        libEmbeddedImage = bitmap.asImageBitmap()
                                    }
                                }
                            }
                        } else if (track.uri != null) {
                            retriever.setDataSource(context, track.uri)
                            val bytes = retriever.embeddedPicture
                            if (bytes != null) {
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    withContext(Dispatchers.Main) {
                                        libEmbeddedImage = bitmap.asImageBitmap()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    } finally {
                        try { retriever.release() } catch (e: Exception) {}
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            if (libEmbeddedImage != null) {
                Image(
                    bitmap = libEmbeddedImage!!,
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (track.coverResId != null) {
                Image(
                    painter = painterResource(id = track.coverResId),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = Color(0xFFE23E57),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = if (isPlayingThis) Color(0xFFE23E57) else Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (track.isHiRes) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Hi-Res",
                        tint = Color(0xFFE23E57),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(text = "Hi-Res", color = Color(0xFFE23E57), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Lossless",
                        tint = Color.Gray,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(text = "Lossless", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "•", color = Color.Gray, fontSize = 10.sp)
                Text(text = track.album, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Playing indicator or Arrow icon
        if (isPlayingThis) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Playing",
                tint = Color(0xFFE23E57),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = Color(0xFF2C2C2C),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    HorizontalDivider(color = Color(0xFF121212), thickness = 0.8.dp)
}
