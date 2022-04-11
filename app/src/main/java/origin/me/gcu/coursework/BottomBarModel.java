package origin.me.gcu.coursework;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Name: Matthew J. Garioch
 *  Student ID: S1713642
 */
public class BottomBarModel extends BottomSheetDialogFragment {
    private BottomSheetListener listener;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_bar_layout, container, false);

        LinearLayout btnCurrentIncidents = v.findViewById(R.id.btnCurrentIncidents);
        LinearLayout btnCurrentRoadworks = v.findViewById(R.id.btnRoadworks);
        LinearLayout btnRoadworksPlanned= v.findViewById(R.id.btnPlannedRoadworks);

        btnCurrentIncidents.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                listener.onClicked(RssItem.current_incidents);
                dismiss();
            }
        });
        btnCurrentRoadworks.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                listener.onClicked(RssItem.current_roadworks);
                dismiss();
            }
        });
        btnRoadworksPlanned .setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                listener.onClicked(RssItem.roadworks_planned);
                dismiss();
            }
        });

        return v;
    }

    public interface BottomSheetListener{
        void onClicked(RssItem rssItemType);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (BottomSheetListener) context;
    }
}
