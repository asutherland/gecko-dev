/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TranslationsState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.translate.Language
import mozilla.components.concept.engine.translate.initialFromLanguage
import mozilla.components.concept.engine.translate.initialToLanguage
import mozilla.components.lib.state.helpers.AbstractBinding
import org.mozilla.fenix.translations.TranslationDialogBottomSheet
import org.mozilla.fenix.translations.TranslationsFlowState

/**
 * A binding for observing [TranslationsState] changes
 * from the [BrowserStore] and updating the translations action button.
 *
 * @param browserStore [BrowserStore] observed for any changes related to [TranslationsState].
 * @param onStateUpdated Invoked when the translations action button should be updated with the new translations state.
 * @param onShowTranslationsDialog Invoked when [TranslationDialogBottomSheet]
 * should be automatically shown to the user.
 */
class TranslationsBinding(
    private val browserStore: BrowserStore,
    private val onStateUpdated: (
        isVisible: Boolean,
        isTranslated: Boolean,
        fromSelectedLanguage: Language?,
        toSelectedLanguage: Language?,
    ) -> Unit,
    private val onShowTranslationsDialog: () -> Unit,
) : AbstractBinding<BrowserState>(browserStore) {

    override suspend fun onState(flow: Flow<BrowserState>) {
        // Browser level flows
        val browserFlow = flow.mapNotNull { state -> state }
            .distinctUntilChangedBy {
                it.translationEngine
            }

        // Session level flows
        val sessionFlow = flow.mapNotNull { state -> state.selectedTab }
            .distinctUntilChangedBy {
                it.translationsState
            }

        // Applying the flows together
        sessionFlow
            .combine(browserFlow) { sessionState, browserState ->
                TranslationsFlowState(
                    sessionState,
                    browserState,
                )
            }
            .collect { state ->
                // Browser Translations State Behavior (Global)
                val browserTranslationsState = state.browserState.translationEngine
                val translateFromLanguages =
                    browserTranslationsState.supportedLanguages?.fromLanguages
                val translateToLanguages =
                    browserTranslationsState.supportedLanguages?.toLanguages
                val isEngineSupported = browserTranslationsState.isEngineSupported

                // Session Translations State Behavior (Tab)
                val sessionTranslationsState = state.sessionState.translationsState

                if (isEngineSupported == true && sessionTranslationsState.isTranslated) {
                    val fromSelected =
                        sessionTranslationsState.translationEngineState?.initialFromLanguage(
                            translateFromLanguages,
                        )
                    val toSelected =
                        sessionTranslationsState.translationEngineState?.initialToLanguage(
                            translateToLanguages,
                        )

                    if (fromSelected != null && toSelected != null) {
                        onStateUpdated(
                            true,
                            true,
                            fromSelected,
                            toSelected,
                        )
                    }
                } else if (isEngineSupported == true && sessionTranslationsState.isExpectedTranslate) {
                    onStateUpdated(
                        true,
                        false,
                        null,
                        null,
                    )
                } else {
                    onStateUpdated(false, false, null, null)
                }

                if (isEngineSupported == true && sessionTranslationsState.isOfferTranslate) {
                    browserStore.dispatch(
                        TranslationsAction.TranslateOfferAction(
                            tabId = state.sessionState.id,
                            isOfferTranslate = false,
                        ),
                    )
                    onShowTranslationsDialog()
                }
            }
    }
}
