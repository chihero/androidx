/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.tv.tvmaterial.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material.ExperimentalTvMaterialApi
import androidx.tv.material.carousel.Carousel
import androidx.tv.material.carousel.CarouselItem

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
fun FeaturedCarousel() {
    val backgrounds = listOf(
        Color.Red.copy(alpha = 0.3f),
        Color.Yellow.copy(alpha = 0.3f),
        Color.Green.copy(alpha = 0.3f)
    )

    Carousel(
        slideCount = backgrounds.size,
        modifier = Modifier
            .height(300.dp)
            .fillMaxWidth(),
    ) { itemIndex ->
        CarouselItem(
            overlayEnterTransitionStartDelayMillis = 0,
            background = {
                Box(
                    modifier = Modifier
                        .background(backgrounds[itemIndex])
                        .border(2.dp, Color.White.copy(alpha = 0.5f))
                        .fillMaxSize()
                )
            }
        ) {
            Card()
        }
    }
}

@Composable
private fun Card() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        var isFocused by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = if (isFocused) Color.Red else Color.Transparent,
                    shape = RoundedCornerShape(50)
                )
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(vertical = 2.dp, horizontal = 5.dp)
            ) {
                Text(text = "Play")
            }
        }
    }
}
