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

package androidx.tv.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * Material Design tab.
 *
 * A default Tab, also known as a Primary Navigation Tab. Tabs organize content across different
 * screens, data sets, and other interactions.
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * @param selected whether this tab is selected or not
 * @param onSelect called when this tab is selected (when in focus). Doesn't trigger if the tab is
 * already selected
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors these will be used by the tab when in different states (focused,
 * selected, etc.)
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this tab. You can create and pass in your own `remember`ed instance to observe [Interaction]s
 * and customize the appearance / behavior of this tab in different states.
 * @param content content of the [Tab]
 */
@Composable
fun Tab(
  selected: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  colors: TabColors = TabDefaults.pillIndicatorTabColors(),
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable RowScope.() -> Unit
) {
  val contentColor by
    animateColorAsState(
      getTabContentColor(
        colors = colors,
        anyTabFocused = LocalTabRowHasFocus.current,
        selected = selected,
        enabled = enabled,
      )
    )
  CompositionLocalProvider(LocalContentColor provides contentColor) {
    Row(
      modifier =
        modifier
          .semantics {
            this.selected = selected
            this.role = Role.Tab
          }
          .onFocusChanged {
            if (it.isFocused && !selected) {
              onSelect()
            }
          }
          .focusable(enabled, interactionSource),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      content = content
    )
  }
}

/**
 * Represents the colors used in a tab in different states.
 *
 * - See [TabDefaults.pillIndicatorTabColors] for the default colors used in a [Tab] when using a
 * Pill indicator.
 * - See [TabDefaults.underlinedIndicatorTabColors] for the default colors used in a [Tab] when
 * using an Underlined indicator
 */
class TabColors
internal constructor(
  private val activeContentColor: Color,
  private val selectedContentColor: Color,
  private val focusedContentColor: Color,
  private val disabledActiveContentColor: Color,
  private val disabledSelectedContentColor: Color,
) {
  /**
   * Represents the content color for this tab, depending on whether it is inactive and [enabled]
   *
   * [Tab] is inactive when the [TabRow] is not focused
   *
   * @param enabled whether the button is enabled
   */
  internal fun inactiveContentColor(enabled: Boolean): Color {
    return if (enabled) activeContentColor.copy(alpha = 0.4f)
    else disabledActiveContentColor.copy(alpha = 0.4f)
  }

  /**
   * Represents the content color for this tab, depending on whether it is active and [enabled]
   *
   * [Tab] is active when some other [Tab] is focused
   *
   * @param enabled whether the button is enabled
   */
  internal fun activeContentColor(enabled: Boolean): Color {
    return if (enabled) activeContentColor else disabledActiveContentColor
  }

  /**
   * Represents the content color for this tab, depending on whether it is selected and [enabled]
   *
   * [Tab] is selected when the current [Tab] is selected and not focused
   *
   * @param enabled whether the button is enabled
   */
  internal fun selectedContentColor(enabled: Boolean): Color {
    return if (enabled) selectedContentColor else disabledSelectedContentColor
  }

  /**
   * Represents the content color for this tab, depending on whether it is focused
   *
   * * [Tab] is focused when the current [Tab] is selected and focused
   */
  internal fun focusedContentColor(): Color {
    return focusedContentColor
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other !is TabColors) return false

    if (activeContentColor != other.activeContentColor(true)) return false
    if (selectedContentColor != other.selectedContentColor(true)) return false
    if (focusedContentColor != other.focusedContentColor()) return false

    if (disabledActiveContentColor != other.activeContentColor(false)) return false
    if (disabledSelectedContentColor != other.selectedContentColor(false)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = activeContentColor.hashCode()
    result = 31 * result + selectedContentColor.hashCode()
    result = 31 * result + focusedContentColor.hashCode()
    result = 31 * result + disabledActiveContentColor.hashCode()
    result = 31 * result + disabledSelectedContentColor.hashCode()
    return result
  }
}

object TabDefaults {
  /**
   * [Tab]'s content colors to in conjunction with underlined indicator
   */
  // TODO: get selected & focused values from theme
  @Composable
  fun underlinedIndicatorTabColors(
    activeContentColor: Color = LocalContentColor.current,
    selectedContentColor: Color = Color(0xFFC9C2E8),
    focusedContentColor: Color = Color(0xFFC9BFFF),
    disabledActiveContentColor: Color = activeContentColor,
    disabledSelectedContentColor: Color = selectedContentColor,
  ): TabColors =
    TabColors(
      activeContentColor = activeContentColor,
      selectedContentColor = selectedContentColor,
      focusedContentColor = focusedContentColor,
      disabledActiveContentColor = disabledActiveContentColor,
      disabledSelectedContentColor = disabledSelectedContentColor,
    )

  /**
   * [Tab]'s content colors to in conjunction with pill indicator
   */
  // TODO: get selected & focused values from theme
  @Composable
  fun pillIndicatorTabColors(
    activeContentColor: Color = LocalContentColor.current,
    selectedContentColor: Color = Color(0xFFE5DEFF),
    focusedContentColor: Color = Color(0xFF313033),
    disabledActiveContentColor: Color = activeContentColor,
    disabledSelectedContentColor: Color = selectedContentColor,
  ): TabColors =
    TabColors(
      activeContentColor = activeContentColor,
      selectedContentColor = selectedContentColor,
      focusedContentColor = focusedContentColor,
      disabledActiveContentColor = disabledActiveContentColor,
      disabledSelectedContentColor = disabledSelectedContentColor,
    )
}

/** Returns the [Tab]'s content color based on focused/selected state */
private fun getTabContentColor(
  colors: TabColors,
  anyTabFocused: Boolean,
  selected: Boolean,
  enabled: Boolean,
): Color =
  when {
    anyTabFocused && selected -> colors.focusedContentColor()
    selected -> colors.selectedContentColor(enabled)
    anyTabFocused -> colors.activeContentColor(enabled)
    else -> colors.inactiveContentColor(enabled)
  }
