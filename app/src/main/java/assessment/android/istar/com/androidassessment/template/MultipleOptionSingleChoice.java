package assessment.android.istar.com.androidassessment.template;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.andexert.library.RippleView;

import assessment.android.istar.com.androidassessment.CMSAssessmentFragment;
import assessment.android.istar.com.androidassessment.R;
import assessment.android.istar.com.androidassessment.assessment_pojo.CMSOption;
import assessment.android.istar.com.androidassessment.assessment_pojo.CMSQuestion;

/**
 * Created by Ajith on 14-12-2016.
 */

public class MultipleOptionSingleChoice extends AssessmentCard {

    private WebView question, passage;
    private TextView option1, option2, option3, option4, option5;
    private AppCompatRadioButton rbtn1, rbtn2, rbtn3, rbtn4, rbtn5;
    private int position;
    private CMSQuestion cmsQuestion;
    private View view;
    private String selectedVal = "";
    private long start_time;
    private boolean chck_1 = false, chck_2 = false, chck_3 = false, chck_4 = false, chck_5 = false;
    private TextView hidden_key, hidden_value, hidden_time;
    private ScrollView mainLayout;
    private RelativeLayout label_view;
    private RippleView layoutBtn1, layoutBtn2, layoutBtn3, layoutBtn4, layoutBtn5;
    private ThemeUtils themeutil;
    private CountDownTimer countDownTimer;
    private Boolean submitCheck = false;
    private LinearLayout linearLayout;
    private boolean hasMaxLen = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        themeutil = new ThemeUtils();
        if (getArguments() != null) {
            if (getArguments().getSerializable(AssessmentCard.CMSASSESSMENT) != null) {
                cmsQuestion = (CMSQuestion) getArguments().getSerializable(AssessmentCard.CMSASSESSMENT);
                if (getArguments().getInt(AssessmentCard.POSITION, -1) != -1)
                    position = getArguments().getInt(AssessmentCard.POSITION, -1);
            }
        }

        if (themeutil.getOptionView(cmsQuestion)) {
            view = inflater.inflate(R.layout.new_multipleoption_singlechoice, container, false);
        } else {
            hasMaxLen = true;
            view = inflater.inflate(R.layout.multipleoption_singlechoice, container, false);
        }


        //hardware acceleration disable
        try {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } catch (Exception e) {
        }


        label_view = (RelativeLayout) view.findViewById(R.id.label_view);
        mainLayout = (ScrollView) view.findViewById(R.id.mainLayout);
        question = (WebView) view.findViewById(R.id.question);
        passage = (WebView) view.findViewById(R.id.passage);
        option1 = (TextView) view.findViewById(R.id.option1);
        option2 = (TextView) view.findViewById(R.id.option2);
        option3 = (TextView) view.findViewById(R.id.option3);
        option4 = (TextView) view.findViewById(R.id.option4);
        option5 = (TextView) view.findViewById(R.id.option5);

        rbtn1 = (AppCompatRadioButton) view.findViewById(R.id.rbtn1);
        rbtn2 = (AppCompatRadioButton) view.findViewById(R.id.rbtn2);
        rbtn3 = (AppCompatRadioButton) view.findViewById(R.id.rbtn3);
        rbtn4 = (AppCompatRadioButton) view.findViewById(R.id.rbtn4);
        rbtn5 = (AppCompatRadioButton) view.findViewById(R.id.rbtn5);

        layoutBtn1 = (RippleView) view.findViewById(R.id.layoutBtn1);
        layoutBtn2 = (RippleView) view.findViewById(R.id.layoutBtn2);
        layoutBtn3 = (RippleView) view.findViewById(R.id.layoutBtn3);
        layoutBtn4 = (RippleView) view.findViewById(R.id.layoutBtn4);
        layoutBtn5 = (RippleView) view.findViewById(R.id.layoutBtn5);


        if (hasMaxLen)
            linearLayout = (LinearLayout) view.findViewById(R.id.lay3);

        Boolean externalReadable = ImageSaver.isExternalStorageReadable();


        hidden_key = (TextView) view.findViewById(R.id.hidden_key);
        hidden_value = (TextView) view.findViewById(R.id.hidden_value);
        hidden_time = (TextView) view.findViewById(R.id.hidden_time);


        if (cmsQuestion != null) {
            if (cmsQuestion.getQuestionText() != null) {
                themeutil.getThemeQuestion(cmsQuestion, question, getActivity(), externalReadable);
            }

            if (cmsQuestion.getComprehensive_passage() != null) {
                themeutil.getThemePassage(cmsQuestion, passage, getActivity(), externalReadable);
            }

            if (cmsQuestion.getOptions() != null) {
                int temp = 0;
                hidden_key.setText(cmsQuestion.getId() + "");

                selectedVal = "";
                for (CMSOption cmsOption : cmsQuestion.getOptions()) {
                    if (temp == 0) {
                        themeutil.getThemeSingleOption(cmsQuestion, option1, rbtn1, layoutBtn1, cmsOption, getActivity(), externalReadable);
                    }
                    if (temp == 1) {
                        themeutil.getThemeSingleOption(cmsQuestion, option2, rbtn2, layoutBtn2, cmsOption, getActivity(), externalReadable);
                    }
                    if (temp == 2) {
                        themeutil.getThemeSingleOption(cmsQuestion, option3, rbtn3, layoutBtn3, cmsOption, getActivity(), externalReadable);
                    }
                    if (temp == 3) {
                        themeutil.getThemeSingleOption(cmsQuestion, option4, rbtn4, layoutBtn4, cmsOption, getActivity(), externalReadable);
                    }
                    if (temp == 4) {
                        themeutil.getThemeSingleOption(cmsQuestion, option5, rbtn5, layoutBtn5, cmsOption, getActivity(), externalReadable);
                        if (hasMaxLen)
                            linearLayout.setVisibility(View.VISIBLE);
                    }
                    temp++;
                }
            }
            if (cmsQuestion.getTheme() != null) {
                selectUnselect(0);
            }
        }
        webviewSetup();
        return view;
    }

    public void createCountDownTimer() {
        try {
            countDownTimer = new CountDownTimer(350, 1000) { // adjust the milli seconds here
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    try {
                        CMSAssessmentFragment.nextViewpager(hidden_key.getText().toString(), selectedVal, ((System.currentTimeMillis() - start_time) / 1000) + "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        } catch (Exception e) {

        }
    }

    public void selectUnselect(int position) {
        switch (position) {
            case 1:
                selectedVal = rbtn1.getTag().toString();
                chck_1 = true;
                chck_2 = false;
                chck_3 = false;
                chck_4 = false;
                chck_5 = false;
                break;
            case 2:
                selectedVal = rbtn2.getTag().toString();

                chck_2 = true;
                chck_1 = false;
                chck_3 = false;
                chck_4 = false;
                chck_5 = false;
                break;
            case 3:
                selectedVal = rbtn3.getTag().toString();

                chck_3 = true;
                chck_1 = false;
                chck_2 = false;
                chck_4 = false;
                chck_5 = false;
                break;
            case 4:
                selectedVal = rbtn4.getTag().toString();

                chck_4 = true;
                chck_1 = false;
                chck_2 = false;
                chck_3 = false;
                chck_5 = false;
                break;
            case 5:
                selectedVal = rbtn5.getTag().toString();

                chck_5 = true;
                chck_1 = false;
                chck_2 = false;
                chck_3 = false;
                chck_4 = false;
                break;
            default:
                selectedVal = "";

                option1.setTextColor(Color.parseColor("#000000"));
                option2.setTextColor(Color.parseColor("#000000"));
                option3.setTextColor(Color.parseColor("#000000"));
                option4.setTextColor(Color.parseColor("#000000"));
                option5.setTextColor(Color.parseColor("#000000"));

              /*  layoutBtn1.setBackgroundColor(Color.parseColor("#ffffff"));
                layoutBtn2.setBackgroundColor(Color.parseColor("#ffffff"));
                layoutBtn3.setBackgroundColor(Color.parseColor("#ffffff"));
                layoutBtn4.setBackgroundColor(Color.parseColor("#ffffff"));
                layoutBtn5.setBackgroundColor(Color.parseColor("#ffffff"));*/
                break;
        }
        hidden_value.setText(selectedVal + "");
    }

    private void webviewSetup() {
        forceWebViewRedraw(layoutBtn1);
        forceWebViewRedraw(layoutBtn2);
        forceWebViewRedraw(layoutBtn3);
        forceWebViewRedraw(layoutBtn4);
        forceWebViewRedraw(layoutBtn5);

        question.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
    }

    private void forceWebViewRedraw(final RippleView rippleView) {


        if (rippleView != null) {
            rippleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!submitCheck) {
                        switch (view.getId()) {
                            case R.id.layoutBtn1:
                                if (chck_1) {
                                    selectUnselect(0);
                                    chck_1 = false;
                                } else {
                                    selectUnselect(1);
                                }
                                break;
                            case R.id.layoutBtn2:
                                if (chck_2) {
                                    selectUnselect(0);
                                    chck_2 = false;
                                } else {
                                    selectUnselect(2);
                                }
                                break;
                            case R.id.layoutBtn3:
                                if (chck_3) {
                                    selectUnselect(0);
                                    chck_3 = false;
                                } else {
                                    selectUnselect(3);
                                }
                                break;
                            case R.id.layoutBtn4:
                                if (chck_4) {
                                    selectUnselect(0);
                                    chck_4 = false;
                                } else {
                                    selectUnselect(4);
                                }
                                break;
                            case R.id.layoutBtn5:
                                if (chck_5) {
                                    selectUnselect(0);
                                    chck_5 = false;
                                } else {
                                    selectUnselect(5);
                                }
                                break;
                        }
                        createCountDownTimer();
                        submitCheck = true;
                    }
                }
            });
        }
    }

    /* mWebView.setOnTouchListener(new View.OnTouchListener() {

            public final static int FINGER_RELEASED = 0;
            public final static int FINGER_TOUCHED = 1;
            public final static int FINGER_DRAGGING = 2;
            public final static int FINGER_UNDEFINED = 3;

            private int fingerState = FINGER_RELEASED;


            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        if (fingerState == FINGER_RELEASED) fingerState = FINGER_TOUCHED;
                        else fingerState = FINGER_UNDEFINED;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (fingerState == FINGER_TOUCHED || fingerState == FINGER_DRAGGING)
                            fingerState = FINGER_DRAGGING;
                        else fingerState = FINGER_UNDEFINED;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (fingerState != FINGER_DRAGGING) {
                            fingerState = FINGER_RELEASED;

                            switch (view.getId()) {
                                case R.id.option1:
                                    if (chck_1) {
                                        selectUnselect(0);
                                        chck_1 = false;
                                    } else {
                                        selectUnselect(1);
                                    }
                                    break;
                                case R.id.option2:
                                    if (chck_2) {
                                        selectUnselect(0);
                                        chck_2 = false;
                                    } else {
                                        selectUnselect(2);
                                    }
                                    break;
                                case R.id.option3:
                                    if (chck_3) {
                                        selectUnselect(0);
                                        chck_3 = false;
                                    } else {
                                        selectUnselect(3);
                                    }
                                    break;
                                case R.id.option4:
                                    if (chck_4) {
                                        selectUnselect(0);
                                        chck_4 = false;
                                    } else {
                                        selectUnselect(4);
                                    }
                                    break;
                                case R.id.option5:
                                    if (chck_5) {
                                        selectUnselect(0);
                                        chck_5 = false;
                                    } else {
                                        selectUnselect(5);
                                    }
                                    break;
                            }
                            if (view.getId() != R.id.question)
                                createCountDownTimer();

                        } else if (fingerState == FINGER_DRAGGING) fingerState = FINGER_RELEASED;
                        else fingerState = FINGER_UNDEFINED;
                        break;
                    default:
                        fingerState = FINGER_UNDEFINED;
                        break;
                }
                return false;
            }
        });*/

    /*private void setColorforRadioButton() {
        try {
            String color = "#000000";
            try {
                color = cmsQuestion.getTheme().getListitemFontColor();
            } catch (Exception e) {
            }
            int[][] states = new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}};
            int[] colors = new int[]{Color.parseColor(color), Color.parseColor(color)};
            ColorStateList colorStateList = new ColorStateList(states, colors);
            CompoundButtonCompat.setButtonTintList(rbtn1, colorStateList);
            CompoundButtonCompat.setButtonTintList(rbtn2, colorStateList);
            CompoundButtonCompat.setButtonTintList(rbtn3, colorStateList);
            CompoundButtonCompat.setButtonTintList(rbtn4, colorStateList);
            CompoundButtonCompat.setButtonTintList(rbtn5, colorStateList);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        try {
            if (isVisibleToUser) {
                start_time = System.currentTimeMillis();
                hidden_time.setText(start_time + "");
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

}
