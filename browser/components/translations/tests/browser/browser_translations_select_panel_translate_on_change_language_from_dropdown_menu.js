/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

"use strict";

/**
 * This test case verifies the behavior of triggering a translation by switching the
 * from-language to a valid selection by opening the language dropdown when the panel
 * is in the "idle" state without valid language pair.
 */
add_task(
  async function test_select_translations_panel_translate_on_change_from_language() {
    const { cleanup, runInPage, resolveDownloads } = await loadTestPage({
      page: SELECT_TEST_PAGE_URL,
      languagePairs: [
        // Do not include Spanish.
        { fromLang: "fr", toLang: "en" },
        { fromLang: "en", toLang: "fr" },
      ],
      prefs: [["browser.translations.select.enable", true]],
    });

    await SelectTranslationsTestUtils.openPanel(runInPage, {
      selectSpanishSection: true,
      openAtSpanishSection: true,
      expectedFromLanguage: null,
      expectedToLanguage: "en",
      onOpenPanel:
        SelectTranslationsTestUtils.assertPanelViewNoFromLangSelected,
    });

    await SelectTranslationsTestUtils.changeSelectedFromLanguage(["fr"], {
      openDropdownMenu: true,
      downloadHandler: resolveDownloads,
      onChangeLanguage: SelectTranslationsTestUtils.assertPanelViewTranslated,
    });

    await cleanup();
  }
);

/**
 * This test case verifies the behavior of triggering a translation by switching the
 * to-language to a valid selection by opening the language dropdown when the panel
 * is in the "idle" state without valid language pair.
 */
add_task(
  async function test_select_translations_panel_translate_on_change_to_language() {
    const { cleanup, runInPage, resolveDownloads } = await loadTestPage({
      page: SELECT_TEST_PAGE_URL,
      languagePairs: LANGUAGE_PAIRS,
      prefs: [["browser.translations.select.enable", true]],
    });

    await SelectTranslationsTestUtils.openPanel(runInPage, {
      selectEnglishSection: true,
      openAtEnglishSection: true,
      expectedFromLanguage: "en",
      expectedToLanguage: null,
      onOpenPanel: SelectTranslationsTestUtils.assertPanelViewNoToLangSelected,
    });

    await SelectTranslationsTestUtils.changeSelectedToLanguage(["es"], {
      openDropdownMenu: true,
      downloadHandler: resolveDownloads,
      onChangeLanguage: SelectTranslationsTestUtils.assertPanelViewTranslated,
    });

    await cleanup();
  }
);
