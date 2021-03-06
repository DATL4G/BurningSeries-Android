package de.datlag.burningseries.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.rubensousa.previewseekbar.PreviewLoader
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import de.datlag.burningseries.R
import de.datlag.burningseries.common.*
import de.datlag.burningseries.databinding.ExoplayerControlsBinding
import de.datlag.burningseries.ui.dialog.TimeEditDialog
import de.datlag.coilifier.ImageLoader
import de.datlag.coilifier.commons.load
import de.datlag.model.burningseries.stream.StreamConfig
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Obfuscate
class BsPlayerView :
    StyledPlayerView,
    StyledPlayerControlView.VisibilityListener,
    LifecycleObserver
{

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val _isLocked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLocked: Boolean
        get() = _isLocked.value

    private val isFullscreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var fullscreenRestored: Boolean = false
    private var fullscreenListener: ((Boolean) -> Unit)? = null

    private val controlsBinding: ExoplayerControlsBinding by lazy {
        val videoControlView = findViewById<View>(R.id.exoplayer_controls)
        ExoplayerControlsBinding.bind(videoControlView)
    }

    private var backPressUnit: (() -> Unit)? = null

    private val lifecycleOwner: LifecycleOwner?
        get() = findViewTreeLifecycleOwner() ?: context.getLifecycleOwner()

    lateinit var config: StreamConfig
    var newConfig: StreamConfig? = null
    private var saveConfigListener: (suspend (StreamConfig, Boolean) -> Boolean)? = null

    private var resumeAfterTimeEditClose: Boolean = true
    private val timeEditDialog = TimeEditDialog({ newConfig ->
        this.newConfig = newConfig
        if (newConfig.hashCode() != config.hashCode() && newConfig.isValidChanged()) {
            this.newConfig?.combineToValid(config)?.let {
                if(it.hashCode() != config.hashCode()) {
                    getSafeScope().launch(Dispatchers.IO) {
                        if (saveConfigListener?.invoke(it, it.isNewEntry(config)) == true) {
                            config = it.newInstance()
                        }
                    }
                }
            }
        }

        if (resumeAfterTimeEditClose) {
            player?.play()
        }
    }) {
        player?.currentPosition
    }

    init {
        initViews()
        setControllerVisibilityListener(this)

        listenLockState()
        listenFullscreenState()
    }

    private fun initViews(): Unit = with(controlsBinding) {
        if (context.packageManager.isTelevision()) {
            lockButton.gone()
            exoFullscreen.invisible()
            timeButton.gone()
        } else {
            lockButton.visible()
            exoFullscreen.visible()
            timeButton.visible()
        }

        backButton.setOnClickListener {
            backPressUnit?.invoke()
        }
        lockButton.setOnClickListener {
            toggleLockState()
        }
        exoFullscreen.setOnClickListener {
            toggleFullscreenState()
        }
        timeButton.setOnClickListener {
            if (timeEditDialog.creationRequired()) {
                timeEditDialog.create(context, newConfig ?: config)
            }
            resumeAfterTimeEditClose = player?.isPlaying ?: true
            player?.pause()
            timeEditDialog.show(player?.duration ?: Long.MAX_VALUE)
        }
    }

    override fun onVisibilityChange(visibility: Int) {
        try {
            setLocked(_isLocked.value)
        } catch (ignored: Throwable) {
            controlsBinding.root.post {
                setLocked(_isLocked.value)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            if (isControllerFullyVisible) {
                hideController()
                setLocked(_isLocked.value)
            } else {
                showController()
                setLocked(_isLocked.value)
            }
            return false
        }
        return true
    }

    private fun getSafeScope(): CoroutineScope {
        return lifecycleOwner?.lifecycleScope ?: GlobalScope
    }

    fun setTitle(title: String?): Unit = with(controlsBinding.title) {
        if (title.isNullOrEmpty()) {
            gone()
        } else {
            text = title
            visible()
        }
    }

    fun setOnBackPressed(callback: () -> Unit) {
        backPressUnit = callback
    }

    fun setLockState(locked: Boolean) {
        _isLocked.forceEmit(locked, getSafeScope())
    }

    fun toggleLockState() {
        setLockState(!_isLocked.value)
    }

    fun setConfigSaveListener(listener: suspend (StreamConfig, Boolean) -> Boolean) {
        saveConfigListener = listener
    }

    private fun listenLockState() = lifecycleOwner?.let {
        _isLocked.launchAndCollectIn(it) { value ->
            setLocked(value)
        }
    }

    fun setPreviewLoader(previewLoader: PreviewLoader?): Unit = with(controlsBinding.exoProgress) {
        this.setPreviewLoader(previewLoader)
    }

    fun setPreviewImage(image: ImageLoader, placeholder: Any? = null): Unit = with(controlsBinding) {
        exoProgress.isEnabled = _isLocked.value
        exoProgress.isClickable = _isLocked.value
        imageView.load<Drawable>(image) {
            placeholder(placeholder ?: image)
            error(placeholder ?: image)
        }
    }

    fun setPreviewEnabled(enabled: Boolean): Unit = with((controlsBinding.exoProgress)) {
        isPreviewEnabled = enabled
        isEnabled = _isLocked.value
        isClickable = _isLocked.value
    }

    fun setFullscreenListener(listener: (Boolean) -> Unit) {
        fullscreenListener = listener
    }

    fun setFullscreenState(fullscreen: Boolean) {
        isFullscreen.forceEmit(fullscreen, getSafeScope())
    }

    fun toggleFullscreenState() {
        setFullscreenState(!isFullscreen.value)
    }

    private fun listenFullscreenState() = lifecycleOwner?.let {
        isFullscreen.launchAndCollectIn(it) { value ->
            setFullScreen(value)
        }
    }

    private fun setLocked(toLocked: Boolean): Unit = with(controlsBinding) {
        if (toLocked) {
            lockButton.load<Drawable>(R.drawable.ic_baseline_lock_24) {
                centerInside()
            }
            exoFullscreen.invisible()
            backButton.gone()
            exoPlayPause.gone()
            exoFfwd.gone()
            exoRew.gone()
            timeButton.gone()
            exoPlayPause.post { exoPlayPause.gone() }
            exoFullscreen.post { exoFullscreen.invisible() }
            backButton.post { backButton.gone() }
            exoFfwd.post { exoFfwd.gone() }
            exoRew.post { exoRew.gone() }
            timeButton.post { timeButton.gone() }
        } else {
            lockButton.load<Drawable>(R.drawable.ic_baseline_lock_open_24) {
                centerInside()
            }
            if (!context.packageManager.isTelevision()) {
                exoFullscreen.visible()
                exoFullscreen.post { exoFullscreen.visible() }
                timeButton.visible()
                timeButton.post { timeButton.visible() }
            } else {
                exoFullscreen.invisible()
                exoFullscreen.post { exoFullscreen.invisible() }
                timeButton.gone()
                timeButton.post { timeButton.gone() }
            }
            backButton.visible()
            backButton.post { backButton.visible() }
            exoPlayPause.visible()
            exoPlayPause.post { exoPlayPause.visible() }
            exoFfwd.visible()
            exoFfwd.post { exoFfwd.visible() }
            exoRew.visible()
            exoRew.post { exoRew.visible() }
        }
        exoFullscreen.isEnabled = !toLocked
        backButton.isEnabled = !toLocked
        exoFfwd.isEnabled = !toLocked
        exoFfwd.isClickable = !toLocked
        exoPlayPause.isEnabled = !toLocked
        exoPlayPause.isClickable = !toLocked
        exoRew.isEnabled = !toLocked
        exoRew.isClickable = !toLocked
        exoProgress.isEnabled = !toLocked
        exoProgress.isClickable = !toLocked
        timeButton.isEnabled = !toLocked
        timeButton.isClickable = !toLocked

        exoFullscreen.post { exoFullscreen.isEnabled = !toLocked }
        backButton.post { backButton.isEnabled = !toLocked }
        exoFfwd.post {
            exoFfwd.isEnabled = !toLocked
            exoFfwd.isClickable = !toLocked
        }
        exoPlayPause.post {
            exoPlayPause.isEnabled = !toLocked
            exoPlayPause.isClickable = !toLocked
        }
        exoRew.post {
            exoRew.isEnabled = !toLocked
            exoRew.isClickable = !toLocked
        }
        exoProgress.post {
            exoProgress.isEnabled = !toLocked
            exoProgress.isClickable = !toLocked
        }
        timeButton.post {
            timeButton.isEnabled = !toLocked
            timeButton.isClickable = !toLocked
        }
    }

    private fun setFullScreen(toFullScreen: Boolean) = with(controlsBinding) {
        if (toFullScreen) {
            exoFullscreen.load<Drawable>(R.drawable.ic_baseline_fullscreen_exit_24) {
                centerInside()
            }
        } else {
            exoFullscreen.load<Drawable>(R.drawable.ic_baseline_fullscreen_24) {
                centerInside()
            }
        }
        fullscreenListener?.invoke(toFullScreen)
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = try {
            super.onSaveInstanceState()
        } catch (ignored: Exception) {
            BaseSavedState.EMPTY_STATE
        }
        val save = try {
            SaveState(
                state,
                _isLocked.value,
                isFullscreen.value,
                config,
                newConfig
            )
        } catch (ignored: Exception) { state }

        return save ?: BaseSavedState.EMPTY_STATE
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val saveState = state as? SaveState?
        super.onRestoreInstanceState(saveState?.superSaveState ?: state)

        saveState?.let { save ->
            _isLocked.forceEmit(save.isLocked, getSafeScope())
            isFullscreen.forceEmit(save.isFullscreen, getSafeScope())
            fullscreenRestored = true
            config = save.streamConfig
            newConfig = save.newStreamConfig
        }
    }

    @Parcelize
    data class SaveState(
        val superSaveState: Parcelable?,
        val isLocked: Boolean,
        val isFullscreen: Boolean,
        val streamConfig: StreamConfig,
        val newStreamConfig: StreamConfig?
    ) : View.BaseSavedState(superSaveState), Parcelable
}