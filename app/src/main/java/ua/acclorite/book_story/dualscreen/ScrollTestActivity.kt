package ua.acclorite.book_story.dualscreen
<!-- Step 0 scroll-sync test (scratch, not for release) -->
        <activity
            android:name=".dualscreen.ScrollTestActivity"
            android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|locale|density|uiMode"
            android:exported="true"
            android:theme="@style/BookStory">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

import android.app.Presentation
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Test data: 500 numbered rows so we can see at a glance which rows are on which panel.
 */
private val ITEMS = (1..500).toList()

class ScrollTestActivity : ComponentActivity() {

    // Variant A: both columns share ONE LazyListState instance.
    // Variant B: companion has its own state, mirrored from primary via snapshotFlow.
    private var useSharedState by mutableStateOf(true)

    private var presentation: Presentation? = null

    // The primary list's state. In shared mode the companion uses this directly.
    private lateinit var primaryState: LazyListState

    // The companion's own state (follower mode only).
    private lateinit var followerState: LazyListState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            primaryState = rememberLazyListState()
            followerState = rememberLazyListState()

            // Follower mode: mirror primary -> follower whenever primary scrolls.
            LaunchedEffect(useSharedState) {
                if (!useSharedState) {
                    snapshotFlow {
                        primaryState.firstVisibleItemIndex to primaryState.firstVisibleItemScrollOffset
                    }.collect { (index, offset) ->
                        // Offset the companion forward by a chunk so it shows the NEXT
                        // section of text, not a duplicate of the top.
                        followerState.scrollToItem(index + COMPANION_ITEM_OFFSET, offset)
                    }
                }
            }

            // (Re)build the companion presentation whenever the mode flips.
            LaunchedEffect(useSharedState) {
                showCompanion(if (useSharedState) primaryState else followerState)
            }

            Column(Modifier.fillMaxSize().background(Color.White)) {
                Row(Modifier.fillMaxWidth().padding(8.dp)) {
                    Button(onClick = { useSharedState = !useSharedState }) {
                        Text(if (useSharedState) "Mode: SHARED (tap=follower)" else "Mode: FOLLOWER (tap=shared)")
                    }
                }
                Text(
                    "PRIMARY (this screen)",
                    Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                NumberedList(
                    state = primaryState,
                    tint = Color(0xFFE3F2FD),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    /** Hosts a second NumberedList on the secondary display, bound to [companionState]. */
    private fun showCompanion(companionState: LazyListState) {
        presentation?.dismiss()
        presentation = null

        val secondary = DisplayInspector.firstSecondary(this) ?: return
        val display: Display = (getSystemService(DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
            .getDisplay(secondary.id) ?: return

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ScrollTestActivity)
            setViewTreeViewModelStoreOwner(this@ScrollTestActivity)
            setViewTreeSavedStateRegistryOwner(this@ScrollTestActivity)
            setContent {
                Column(Modifier.fillMaxSize().background(Color.White)) {
                    Text(
                        "COMPANION (display ${secondary.id})",
                        Modifier.padding(horizontal = 12.dp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    NumberedList(
                        state = companionState,
                        tint = Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        presentation = Presentation(this, display).apply {
            setContentView(composeView)
            show()
        }
    }

    override fun onDestroy() {
        presentation?.dismiss()
        presentation = null
        super.onDestroy()
    }

    companion object {
        // How far ahead the companion starts, in items, in follower mode.
        // Rough stand-in for "one top-panel height" until we measure real geometry.
        const val COMPANION_ITEM_OFFSET = 12
    }
}

@Composable
private fun NumberedList(
    state: LazyListState,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = state, modifier = modifier.background(tint)) {
        items(ITEMS, key = { it }) { n ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text("Line $n", fontSize = 22.sp, color = Color.Black)
            }
        }
    }
}
