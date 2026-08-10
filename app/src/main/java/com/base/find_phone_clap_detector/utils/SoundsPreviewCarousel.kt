package com.base.find_phone_clap_detector.utils

import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass

@Composable
fun SoundsPreviewCarousel(
    sounds: List<SoundsDataClass>,
    initialSelectedIndex: Int = 0,
    onItemSelected: (SoundsDataClass) -> Unit
) {
    val safeInitialIndex = if (sounds.isEmpty()) {
        0
    } else {
        initialSelectedIndex.coerceIn(0, sounds.lastIndex)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    var selectedItem by remember { mutableIntStateOf(safeInitialIndex) }

    val selectedBlue = colorResource(id = R.color.primary)
    val selectedBackground = colorResource(id = R.color.text_card_bg)
    val unselectedBackground = colorResource(id = R.color.sound_preview_surface)
    val unselectedBorder = colorResource(id = R.color.sound_preview_outline)
    val selectedText = colorResource(id = R.color.primary)
    val unselectedText = colorResource(id = R.color.sound_preview_text)
    val badgeStart = colorResource(id = R.color.purple_dark)
    val badgeEnd = colorResource(id = R.color.purple_gradient_light)

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(sounds) { index, sound ->
            val isSelected = index == selectedItem

            Column(
                modifier = Modifier
                    .width(102.dp)
                    .scale(if (isSelected) 1f else 0.96f)
                    .clickable {
                        selectedItem = index
                        onItemSelected(sound)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val safeResId = remember(sound.img) {
                        if (sound.img == 0) {
                            R.drawable.ic_record_audio
                        } else {
                            try {
                                val resourceType = context.resources.getResourceTypeName(sound.img)
                                val typedValue = TypedValue()
                                context.resources.getValue(sound.img, typedValue, true)
                                val resourcePath = typedValue.string?.toString()?.lowercase().orEmpty()
                                val isBitmapDrawable = resourceType == "drawable" &&
                                    (resourcePath.endsWith(".png") ||
                                        resourcePath.endsWith(".jpg") ||
                                        resourcePath.endsWith(".jpeg") ||
                                        resourcePath.endsWith(".webp"))

                                if (isBitmapDrawable) sound.img else R.drawable.ic_record_audio
                            } catch (_: Exception) {
                                R.drawable.ic_record_audio
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = if (isSelected) 8.dp else 1.dp,
                                shape = CircleShape,
                                clip = false
                            )
                            .background(
                                color = if (isSelected) selectedBackground else unselectedBackground,
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) selectedBlue else unselectedBorder,
                                shape = CircleShape
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = safeResId),
                            contentDescription = sound.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(if (isSelected) 52.dp else 47.dp)
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(badgeStart, badgeEnd)
                                    ),
                                    shape = CircleShape
                                )
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.baseline_check_24),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = sound.title,
                    color = if (isSelected) selectedText else unselectedText,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 3.dp, end = 3.dp)
                )
            }
        }
    }

    LaunchedEffect(selectedItem, sounds.size) {
        if (sounds.isNotEmpty()) {
            listState.animateScrollToItem(selectedItem.coerceIn(0, sounds.lastIndex))
        }
    }
}
