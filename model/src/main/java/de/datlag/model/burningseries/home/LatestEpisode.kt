package de.datlag.model.burningseries.home

import android.os.Parcelable
import androidx.room.*
import de.datlag.model.burningseries.Cover
import de.datlag.model.burningseries.HrefTitleBuilder
import de.datlag.model.burningseries.common.encodeToHref
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.datetime.Clock
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Entity(
	tableName = "LatestEpisodeTable",
	indices = [
		Index("latestEpisodeId"),
		Index("href", unique = true)
	]
)
@Obfuscate
data class LatestEpisode(
	@ColumnInfo(name = "title") @SerialName("title") val title: String = String(),
	@ColumnInfo(name = "href") @SerialName("href") override val href: String = String(),
	@ColumnInfo(name = "info", defaultValue = "") @SerialName("infoText") val infoText: String = String(),
	@ColumnInfo(name = "updatedAt") var updatedAt: Long = Clock.System.now().epochSeconds,
	@ColumnInfo(name = "nsfw", defaultValue = "false") @SerialName("isNsfw") val nsfw: Boolean = false,
	@Ignore @SerialName("cover") val cover: Cover = Cover(),
	@Ignore @SerialName("infoFlags") val infoFlags: List<LatestEpisodeInfoFlags>
) : Parcelable, HrefTitleBuilder() {

	@PrimaryKey(autoGenerate = true)
	@IgnoredOnParcel
	@ColumnInfo(name = "latestEpisodeId")
	var latestEpisodeId: Long = 0L

	constructor(
		title: String = String(),
		href: String = String(),
		infoText: String = String(),
		updatedAt: Long = Clock.System.now().epochSeconds,
		nsfw: Boolean = false
	) : this(title, href, infoText, updatedAt, nsfw, Cover(), listOf())

	fun getEpisodeAndSeries(): Pair<String, String> {
		val match = Regex(
			"^(.+(:|\\|))(.+)\$",
			setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
		).find(title)
		
		return Pair(
			match?.groupValues?.get(1)?.trim()?.dropLast(1) ?: String(),
			match?.groupValues?.get(3)?.trim() ?: String()
		)
	}

	override fun hrefTitleFallback(): String {
		return getEpisodeAndSeries().first.encodeToHref()
	}

	fun getHrefWithoutEpisode(): String {
		val hrefSplit = href.split('/')
		return hrefSplit.subList(0, 3).joinToString("/")
	}

	val isJapanese: Boolean
		get() = infoFlags.any { it.isJapanese }

	val isGerman: Boolean
		get() = infoFlags.any { it.isGerman }

	val isEnglish: Boolean
		get() = infoFlags.any { it.isEnglish }

	val isJapaneseSub: Boolean
		get() = infoFlags.any { it.isJapaneseSub }

	val isGermanSub: Boolean
		get() = infoFlags.any { it.isGermanSub }

	val isEnglishSub: Boolean
		get() = infoFlags.any { it.isEnglishSub }
}
