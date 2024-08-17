import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.bottom = verticalSpaceHeight

        // Optional: If you want to add space at the top of the first item
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = verticalSpaceHeight
        }
    }
}
