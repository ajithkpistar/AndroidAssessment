package assessment.android.istar.com.androidassessment.template;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import assessment.android.istar.com.androidassessment.R;


public class DefaultFragment extends AssessmentCard {

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_default, container, false);

        return view;
    }


}
