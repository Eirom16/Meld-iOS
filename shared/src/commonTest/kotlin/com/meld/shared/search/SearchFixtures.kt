package com.meld.shared.search

/** Fixtures compartidos con forma real de SearchResponse (Fase 02C). */
internal object SearchFixtures {
    internal fun run(text: String, browseId: String? = null, pageType: String? = null): String {
        val endpoint =
            if (browseId == null) {
                "null"
            } else {
                val config =
                    if (pageType == null) {
                        "null"
                    } else {
                        """{"browseEndpointContextMusicConfig":{"pageType":"$pageType"}}"""
                    }
                """{"browseEndpoint":{"browseId":"$browseId","params":null,"browseEndpointContextSupportedConfigs":$config}}"""
            }
        return """{"text":"$text","navigationEndpoint":$endpoint}"""
    }

    internal fun flexColumn(vararg runs: String): String =
        """{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[${runs.joinToString(",")}]}}}"""

    internal fun thumbnail(url: String): String =
        """{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"$url","width":60,"height":60}]},"thumbnailCrop":null,"thumbnailScale":null},"musicAnimatedThumbnailRenderer":null,"croppedSquareThumbnailRenderer":null}"""

    internal fun songRenderer(
        videoId: String,
        title: String,
        subtitleRuns: String,
        thumb: String = "https://img/song.jpg",
        explicit: Boolean = false,
    ): String {
        val badges =
            if (explicit) {
                """[{"musicInlineBadgeRenderer":{"icon":{"iconType":"MUSIC_EXPLICIT_BADGE"}}}]"""
            } else {
                "null"
            }
        return """{
          "badges":$badges,"fixedColumns":null,
          "flexColumns":[${flexColumn("""{"text":"$title","navigationEndpoint":{"watchEndpoint":{"videoId":"$videoId","playlistId":null,"playlistSetVideoId":null,"params":null,"index":null,"watchEndpointMusicSupportedConfigs":null}}}""")},${flexColumn(subtitleRuns)}],
          "thumbnail":${thumbnail(thumb)},"menu":null,
          "playlistItemData":{"playlistSetVideoId":null,"videoId":"$videoId"},
          "overlay":null,"navigationEndpoint":null
        }"""
    }

    internal val sep = run(" • ")

    internal val fullSongSubtitle =
        listOf(
            run("Full Artist", "UCartist1"),
            sep,
            run("Full Album", "MPREalbum1", "MUSIC_PAGE_TYPE_ALBUM"),
            sep,
            run("3:45"),
        ).joinToString(",")

    internal val minimalSongSubtitle =
        listOf(run("Solo Artist", "UCartist2"), sep, run("2:07")).joinToString(",")

    internal fun shelf(vararg renderers: String, continuations: String = "null"): String =
        """{
          "title":{"runs":[{"text":"Songs","navigationEndpoint":null}]},
          "contents":[${renderers.joinToString(",") { """{"musicResponsiveListItemRenderer":$it,"musicMultiRowListItemRenderer":null,"continuationItemRenderer":null}""" }}],
          "continuations":$continuations,"bottomEndpoint":null,"moreContentButton":null
        }"""

    internal fun searchResponse(vararg shelves: String): String {
        val contents =
            shelves.joinToString(",") { shelfJson ->
                """{"musicCarouselShelfRenderer":null,"musicShelfRenderer":$shelfJson,"musicCardShelfRenderer":null,"musicPlaylistShelfRenderer":null,"musicDescriptionShelfRenderer":null,"musicResponsiveHeaderRenderer":null,"musicEditablePlaylistDetailHeaderRenderer":null,"gridRenderer":null,"itemSectionRenderer":null}"""
            }
        return """{
          "contents":{"tabbedSearchResultsRenderer":{"tabs":[{"tabRenderer":{"title":null,"content":{"sectionListRenderer":{"header":null,"contents":[$contents],"continuations":null},"musicQueueRenderer":null},"endpoint":null}}]}},
          "continuationContents":null
        }"""
    }
}
