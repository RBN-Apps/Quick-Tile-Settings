package com.rbn.qtsettings.feature.onboarding.presentation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rbn.qtsettings.R
import com.rbn.qtsettings.core.designsystem.AppButton
import com.rbn.qtsettings.core.designsystem.AppButtonStyle
import com.rbn.qtsettings.core.designsystem.theme.AppSpacing
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingFlow
import com.rbn.qtsettings.feature.onboarding.domain.OnboardingStep
import kotlin.math.absoluteValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** First run, outside the app shell: no rail and no top bar compete with the single next step. */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    actions: OnboardingUiActions,
    modifier: Modifier = Modifier
) {
    val granted = state.granted
    val pages = remember(granted) { OnboardingFlow.pages(granted) }
    val pager = rememberPagerState(OnboardingFlow.indexOf(state.step, granted)) { pages.size }
    val scope = rememberCoroutineScope()
    // The chrome follows the page the pager came to rest on, so a cancelled swipe never moves the
    // indicator and the buttons do not change under a finger that is still deciding.
    val settled = pager.settledPage.coerceIn(pages.indices)
    val visible = pages[settled]
    val target = OnboardingFlow.indexOf(state.step, granted)

    // A page opened by the flow itself, such as a chosen method or an arriving permission, moves
    // the pager. A page the finger is on keeps its gesture.
    LaunchedEffect(target, pages) {
        if (pager.currentPage != target && !pager.isScrollInProgress) pager.turnTo(target)
    }
    LaunchedEffect(pager, pages) {
        snapshotFlow { pager.settledPage }.collect { page -> pages.getOrNull(page)?.let(actions::show) }
    }
    OnboardingBackGesture(pager)

    val colors = MaterialTheme.colorScheme
    // Mixed instead of blended with alpha: the window behind this page is not the app surface.
    val top = lerp(colors.surface, colors.primaryContainer, 0.45f)
    Surface(
        modifier.fillMaxSize().background(Brush.verticalGradient(0f to top, 0.45f to colors.surface)),
        color = Color.Transparent,
        contentColor = colors.onSurface
    ) {
        val onForward = {
            scope.launch { pager.turnTo((pager.currentPage + 1).coerceAtMost(pages.lastIndex)) }
            Unit
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding().testTag(ONBOARDING_TAG)) {
            OnboardingBar(visible, settled + 1, pages.size,
                onBack = if (settled == 0) null
                    else ({ scope.launch { pager.turnTo(pager.currentPage - 1) } }),
                onSkip = actions::skip)
            OnboardingPages(state, actions, pages, pager, onForward, Modifier.weight(1f))
            OnboardingActionBar(pager, pages, state, actions, onForward)
        }
    }
}

/**
 * The system gesture drags the previous page in under the finger and commits to it, so Back and a
 * swipe end in the same place. Releasing early leaves the current page where it was.
 */
@Composable
private fun OnboardingBackGesture(pager: PagerState) {
    PredictiveBackHandler(enabled = pager.settledPage > 0) { progress ->
        val from = pager.settledPage
        try {
            pager.scroll {
                var applied = 0f
                progress.collect { event ->
                    val offset = -pager.layoutInfo.pageSize * event.progress
                    scrollBy(offset - applied)
                    applied = offset
                }
            }
            pager.turnTo((from - 1).coerceAtLeast(0))
        } catch (_: CancellationException) {
            pager.turnTo(from)
        }
    }
}

@Composable
private fun OnboardingPages(
    state: OnboardingUiState,
    actions: OnboardingUiActions,
    pages: List<OnboardingStep>,
    pager: PagerState,
    onForward: () -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier) {
        // Landscape and large fonts leave too little room for the artwork above the content.
        CompositionLocalProvider(LocalOnboardingShortPage provides (maxHeight < 420.dp)) {
            HorizontalPager(pager, Modifier.fillMaxSize().testTag(ONBOARDING_PAGER_TAG)) { page ->
                Column(Modifier.fillMaxSize().onboardingPageDepth(pager, page)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PageGutter, vertical = AppSpacing.sectionGap),
                    verticalArrangement = if (pages[page] == OnboardingStep.Welcome) Arrangement.Center
                        else Arrangement.spacedBy(AppSpacing.sectionGap)) {
                    when (pages[page]) {
                        OnboardingStep.Welcome -> OnboardingWelcomePage(state.decorativeMotion, onForward)
                        OnboardingStep.Tiles -> OnboardingTilesPage()
                        OnboardingStep.Permission -> OnboardingPermissionPage(state, actions::chooseMethod)
                        OnboardingStep.Grant -> OnboardingGrantPage(state, actions)
                        OnboardingStep.Done -> OnboardingDonePage(state, actions::addTile)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBar(
    visible: OnboardingStep,
    position: Int,
    count: Int,
    onBack: (() -> Unit)?,
    onSkip: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(AppSpacing.contentGap), verticalAlignment = Alignment.CenterVertically) {
        BackSlot(onBack)
        OnboardingProgress(position, count)
        Spacer(Modifier.weight(1f))
        // Leaving early is offered while nothing is at stake; the setup pages end in "later" instead.
        if (visible == OnboardingStep.Welcome || visible == OnboardingStep.Tiles)
            AppButton(stringResource(R.string.onboarding_skip), onSkip,
                Modifier.testTag(ONBOARDING_SKIP_TAG), style = AppButtonStyle.Text)
    }
}

/** Its own composable so the enclosing row's scope does not capture the visibility animation. */
@Composable
private fun BackSlot(onBack: (() -> Unit)?) {
    Box(Modifier.size(AppSpacing.minimumTouchTarget), Alignment.Center) {
        AnimatedVisibility(onBack != null, enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f)) {
            IconButton({ onBack?.invoke() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_back_desc))
            }
        }
    }
}

/**
 * The bar belongs to the page under it, so it leaves with that page instead of cross-fading into
 * the next label. Its opacity follows the pager: the outgoing button is gone within the first
 * third of the move, the label swaps while nothing is on screen, and the incoming one arrives with
 * its page. A reserved minimum height keeps the pages above from shifting as the bar changes.
 */
@Composable
private fun OnboardingActionBar(
    pager: PagerState,
    pages: List<OnboardingStep>,
    state: OnboardingUiState,
    actions: OnboardingUiActions,
    onForward: () -> Unit
) {
    // The welcome page carries its own start button next to the artwork, so the bar stays empty
    // there. Both setup pages leave the flow the moment Android reports the permission, so neither
    // ever has to offer a way past an answer the user has already given.
    val page = pages[pager.currentPage.coerceIn(pages.indices)]
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer {
                alpha = 1f - (pager.currentPageOffsetFraction.absoluteValue * 2f).coerceIn(0f, 1f)
            }
            .heightIn(min = ActionBarMinHeight)
            .padding(horizontal = PageGutter, vertical = AppSpacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.contentGap)
    ) {
        when (page) {
            OnboardingStep.Welcome -> Unit
            OnboardingStep.Tiles -> Primary(R.string.onboarding_next, onForward)
            OnboardingStep.Permission -> Secondary(R.string.onboarding_later, actions::later)
            OnboardingStep.Grant -> {
                Primary(R.string.permission_refresh, actions::refresh, enabled = !state.busy)
                Secondary(R.string.onboarding_later, actions::later)
            }
            OnboardingStep.Done -> Primary(R.string.onboarding_finish, actions::finish)
        }
    }
}

@Composable
private fun Primary(@StringRes label: Int, onClick: () -> Unit, enabled: Boolean = true) =
    AppButton(stringResource(label), onClick,
        Modifier.fillMaxWidth().testTag(ONBOARDING_PRIMARY_TAG), enabled = enabled)

@Composable
private fun Secondary(@StringRes label: Int, onClick: () -> Unit) =
    AppButton(stringResource(label), onClick, Modifier.fillMaxWidth(), style = AppButtonStyle.Text)

/** Wider than the 16.dp settings rows: a full-screen page reads better with more air. */
private val PageGutter = 24.dp

/** One button plus its padding, reserved so an empty bar does not resize the page above it. */
private val ActionBarMinHeight = AppSpacing.minimumTouchTarget + AppSpacing.sectionGap * 2

/**
 * Deliberate enough for the action bar to fade out and back in within the move. The default spring
 * lands in about a tenth of a second, which turns that fade into a blink.
 */
private suspend fun PagerState.turnTo(page: Int) =
    animateScrollToPage(page, animationSpec = tween(320, easing = FastOutSlowInEasing))

internal const val ONBOARDING_TAG = "onboarding"
internal const val ONBOARDING_PAGER_TAG = "onboarding_pager"
internal const val ONBOARDING_SKIP_TAG = "onboarding_skip"
internal const val ONBOARDING_START_TAG = "onboarding_start"
internal const val ONBOARDING_PRIMARY_TAG = "onboarding_primary"
