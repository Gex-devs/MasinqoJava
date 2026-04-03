package gex.com.masinqojava

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments: ArrayList<Fragment>
    private val titles: ArrayList<String?>

    init {
        this.fragments = ArrayList<Fragment>()
        this.titles = ArrayList<String?>()
    }

    fun addFragment(fragment: Fragment?, title: String?) {
        fragments.add(fragment!!)
        titles.add(title)
    }

    override fun createFragment(position: Int): Fragment {
        return fragments.get(position)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    fun getTitle(position: Int): String? {
        return titles.get(position)
    }
}
