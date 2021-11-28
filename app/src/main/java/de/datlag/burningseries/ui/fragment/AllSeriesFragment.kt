package de.datlag.burningseries.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hadiyarajesh.flower.Resource
import dagger.hilt.android.AndroidEntryPoint
import de.datlag.burningseries.R
import de.datlag.burningseries.adapter.AllSeriesRecyclerAdapter
import de.datlag.burningseries.common.safeContext
import de.datlag.burningseries.databinding.FragmentAllSeriesBinding
import de.datlag.burningseries.extend.AdvancedFragment
import de.datlag.burningseries.viewmodel.BurningSeriesViewModel
import de.datlag.model.burningseries.allseries.GenreModel
import io.michaelrocks.paranoid.Obfuscate
import timber.log.Timber

@AndroidEntryPoint
@Obfuscate
class AllSeriesFragment : AdvancedFragment(R.layout.fragment_all_series) {

    private val binding: FragmentAllSeriesBinding by viewBinding()
    private val burningSeriesViewModel: BurningSeriesViewModel by activityViewModels()

    private val allSeriesRecyclerAdapter = AllSeriesRecyclerAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecycler()
        burningSeriesViewModel.allSeries.launchAndCollect { res ->
            if (res.status == Resource.Status.SUCCESS) {
                res.data?.flatMap {
                    it.toGenreModel()
                }?.let { allSeriesRecyclerAdapter.submitList(it) }
            }
        }
    }

    private fun initRecycler(): Unit = with(binding) {
        allSeriesRecycler.layoutManager = LinearLayoutManager(safeContext)
        allSeriesRecycler.adapter = allSeriesRecyclerAdapter
        allSeriesRecycler.isNestedScrollingEnabled = false

        allSeriesRecyclerAdapter.setOnClickListener { _, item ->
            if (item is GenreModel.GenreItem) {
                findNavController().navigate(AllSeriesFragmentDirections.actionAllSeriesFragmentToSeriesFragment(genreItem = item))
            }
        }
    }
}