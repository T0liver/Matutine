package hu.toliver.matutine.edition

internal object BundledSources {
    val feeds: List<String> = listOf(
        """
        <?xml version="1.0"?>
        <rss version="2.0">
          <channel>
            <title>Sample Wire</title>
            <item>
              <title>Central banks signal a longer pause on rates</title>
              <link>https://example.com/sample/lead</link>
              <description>Officials emphasize data dependence as inflation cools unevenly across regions.</description>
            </item>
          </channel>
        </rss>
        """.trimIndent(),
        """
        <?xml version="1.0"?>
        <rss version="2.0">
          <channel>
            <title>Sample Herald</title>
            <item>
              <title>Tech firms outline cautious hiring plans</title>
              <link>https://example.com/sample/secondary</link>
              <description>Quarterly updates point to selective growth rather than broad expansion.</description>
            </item>
          </channel>
        </rss>
        """.trimIndent(),
        """
        <?xml version="1.0"?>
        <rss version="2.0">
          <channel>
            <title>City Desk</title>
            <item>
              <title>Brief: Transit upgrades set for weekend</title>
              <link>https://example.com/sample/note</link>
              <description>A single track closure may add fifteen minutes to cross-town trips.</description>
            </item>
          </channel>
        </rss>
        """.trimIndent(),
    )

    val calendar: String =
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        SUMMARY:Team standup
        DTSTART:090000
        END:VEVENT
        BEGIN:VEVENT
        SUMMARY:Lunch with Alex
        DTSTART:123000
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    fun feedItems(): List<FeedItem> = feeds.flatMap(::parseRss)

    fun events(): List<EditionEvent> = parseIcs(calendar)
}
