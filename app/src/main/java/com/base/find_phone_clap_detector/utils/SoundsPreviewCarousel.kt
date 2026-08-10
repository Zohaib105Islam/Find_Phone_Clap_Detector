package com.base.find_phone_clap_detector.utils

import android.util.Log
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import com.base.find_phone_clap_detector.R
import com.base.find_phone_clap_detector.ui.dataClasses.SoundsDataClass
import com.base.find_phone_clap_detector.utils.AdsCounter.isAppPremium
import kotlin.math.absoluteValue

@Composable
fun SoundsPreviewCarousel(
    isFromAudio: Boolean,
    sounds: List<SoundsDataClass>,
    initialSelectedIndex: Int = 0,
    onItemSelected: (SoundsDataClass) -> Unit
) {
    val listState = rememberLazyListState()
    var selectedItem by remember { mutableIntStateOf(initialSelectedIndex) }

    val density = LocalDensity.current
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    val itemWidth = 140.dp
    val itemWidthPx = with(density) { itemWidth.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clipToBounds()
    ) {

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy((-30).dp)
        ) {
            itemsIndexed(sounds) { index, sound ->

                val offsetFromCenter = (index - selectedItem).toFloat()
                val distance = offsetFromCenter.absoluteValue

                val scale = (1f - (distance * 0.2f)).coerceIn(0.7f, 1f)
                val zIndexValue = 1f - (distance * 0.1f)

                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .scale(scale)
                        .zIndex(zIndexValue)
                        .clickable {
                            selectedItem = index
                            onItemSelected(sound)
                        },
                    contentAlignment = Alignment.Center
                ) {

                    // MAIN CARD
                    Box(
                        modifier = Modifier
                            .height(140.dp)
                            .padding(2.dp)
                            .border(
                                width = 1.dp,
                                color = if (index == selectedItem)
                                    Color(0xFFBA76FF)
                                else Color(0xFFE4E4E4),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (index == selectedItem)
                                    Color(0xFFF3E8FF)
                                else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp)
                    ) {

                        ConstraintLayout(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            val (imgSound, txtTitle, premiumBadge) = createRefs()

                            // PREMIUM BADGE (top-left)
                            if (sound.isPremium) {
                                if (!isAppPremium()) {
                                    if (!isFromAudio) {
                                        Image(
                                            painter = painterResource(id = R.drawable.crown),
                                            contentDescription = "Premium",
                                            modifier = Modifier
                                                .size(28.dp)
                                                .graphicsLayer { scaleX = -1f }
                                                .constrainAs(premiumBadge) {
                                                    top.linkTo(parent.top)
                                                    start.linkTo(parent.start)
                                                }
                                        )
                                    }
                                }
                            }

                            val context = androidx.compose.ui.platform.LocalContext.current

                            val safeResId = remember(sound.img) {
                                if (sound.img != 0) {
                                    try {
                                        val resourceName = context.resources.getResourceEntryName(sound.img)
                                        val resourceType = context.resources.getResourceTypeName(sound.img)
                                        Log.d("CCC", "Name of drawable is: ${sound.img} and default : ${R.drawable.ic_record_audio}")

                                        // Check if the resource is an image type (png, jpg/jpeg, webp)
                                        val isImageDrawable = if (resourceType == "drawable") {
                                            val typedValue = TypedValue()
                                            context.resources.getValue(sound.img, typedValue, true)
                                            val resourcePath = typedValue.string?.toString()?.lowercase() ?: ""
                                            resourcePath.endsWith(".png") ||
                                                    resourcePath.endsWith(".jpg") ||
                                                    resourcePath.endsWith(".jpeg") ||
                                                    resourcePath.endsWith(".webp")
                                        } else {
                                            false
                                        }

                                        if (isImageDrawable) sound.img else R.drawable.ic_record_audio
                                    } catch (e: Exception) {
                                        R.drawable.ic_record_audio
                                    }
                                } else {
                                    R.drawable.ic_record_audio
                                }
                            }

                            Image(
                                painter = painterResource(id = safeResId),
                                contentDescription = sound.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(50.dp)
                                    .constrainAs(imgSound) {
                                        top.linkTo(parent.top, margin = 16.dp)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                    }
                            )

                            Text(
                                text = sound.title,
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier = Modifier.constrainAs(txtTitle) {
                                    top.linkTo(imgSound.bottom, margin = 4.dp)
                                    start.linkTo(parent.start, margin = 6.dp)
                                    end.linkTo(parent.end, margin = 6.dp)
                                }
                            )
                        }
                    }

                    // SELECTED DOT
                    if (index == selectedItem) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_selected_dot),
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .zIndex(10f)
                        )
                    }
                }
            }
        }
    }


    LaunchedEffect(selectedItem) {
        val centerOffset = (screenWidthPx / 2 - itemWidthPx / 2).toInt()
        listState.animateScrollToItem(
            index = selectedItem,
            scrollOffset = -centerOffset
        )
    }
}
