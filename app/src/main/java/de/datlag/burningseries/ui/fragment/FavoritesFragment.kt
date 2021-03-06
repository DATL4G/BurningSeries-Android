package de.datlag.burningseries.ui.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.ferfalk.simplesearchview.SimpleSearchView
import dagger.hilt.android.AndroidEntryPoint
import de.datlag.burningseries.R
import de.datlag.burningseries.adapter.FavoriteRecyclerAdapter
import de.datlag.burningseries.common.*
import de.datlag.burningseries.databinding.FragmentFavoritesBinding
import de.datlag.burningseries.extend.AdvancedFragment
import de.datlag.burningseries.viewmodel.BurningSeriesViewModel
import io.michaelrocks.paranoid.Obfuscate

@AndroidEntryPoint
@Obfuscate
class FavoritesFragment : AdvancedFragment(R.layout.fragment_favorites) {

    private val binding: FragmentFavoritesBinding by viewBinding()
    private val burningSeriesViewModel: BurningSeriesViewModel by activityViewModels()

    private val favoritesRecyclerAdapter by lazy {
        FavoriteRecyclerAdapter(coversDir, blurHash)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecycler()
        initSearchView()

        burningSeriesViewModel.favorites.launchAndCollect {
            favoritesRecyclerAdapter.submitList(it) {
                binding.favoritesRecycler.scrollToPosition(0)
            }
        }
        burningSeriesViewModel.getAllFavorites()
    }

    private fun initRecycler(): Unit = with(binding) {
        val gridSpanCount = if (isTelevision && isOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
            4
        } else if (!isTelevision && isOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
            3
        } else {
            2
        }
        favoritesRecycler.layoutManager = GridLayoutManager(safeContext, gridSpanCount)
        favoritesRecycler.isNestedScrollingEnabled = false
        favoritesRecycler.adapter = favoritesRecyclerAdapter

        favoritesRecyclerAdapter.setOnClickListener { item ->
            findNavController().safeNavigate(FavoritesFragmentDirections.actionFavoritesFragmentToSeriesFragment(seriesWithInfo = item))
        }
    }

    private fun initSearchView(): Unit = with(binding) {
        searchView?.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isNotEmpty()) {
                    burningSeriesViewModel.searchFavorites(newText)
                } else {
                    burningSeriesViewModel.getAllFavorites()
                }
                return false
            }

            override fun onQueryTextCleared(): Boolean {
                burningSeriesViewModel.getAllFavorites()
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                burningSeriesViewModel.searchFavorites(query)
                return false
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.favorites_menu, menu)
        val item = menu.findItem(R.id.action_search)
        searchView?.setMenuItem(item)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onResume() {
        super.onResume()
        burningSeriesViewModel.cancelFetchSeries()
        burningSeriesViewModel.setSeriesData(null)
    }

    override fun initActivityViews() {
        super.initActivityViews()

        exitFullScreen()
        hideSeriesArc()
        extendedFab?.gone()
        hideNavigationFabs()
        setHasOptionsMenu(true)
        showToolbarBackButton()
        hideSeriesArc()
        setToolbarTitle(R.string.favorites)
        appBarLayout?.setExpanded(false, false)
        appBarLayout?.setExpandable(false)
    }
}