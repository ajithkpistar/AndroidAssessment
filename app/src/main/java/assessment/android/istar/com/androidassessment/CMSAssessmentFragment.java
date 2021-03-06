package assessment.android.istar.com.androidassessment;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import assessment.android.istar.com.androidassessment.assessment_database.AssessmentDataHandler;
import assessment.android.istar.com.androidassessment.assessment_database.AssessmentStatusHandler;
import assessment.android.istar.com.androidassessment.assessment_pojo.CMSAssessment;
import assessment.android.istar.com.androidassessment.assessment_pojo.CMSQuestion;
import assessment.android.istar.com.androidassessment.assessment_result.CMSAssessmentResult;
import assessment.android.istar.com.androidassessment.assessment_result.Entry;
import assessment.android.istar.com.androidassessment.assessment_util.AssessmentLockableViewPager;
import assessment.android.istar.com.androidassessment.assessment_util.SubmitAssessmentAsyncTask;
import assessment.android.istar.com.androidassessment.assessment_util.ViewpagerAdapter;
import assessment.android.istar.com.androidassessment.istarindia.utils.SingletonStudent;
import assessment.android.istar.com.androidassessment.template.MultipleOptionMultipleChoice;
import assessment.android.istar.com.androidassessment.template.MultipleOptionSingleChoice;
import me.itangqi.waveloadingview.WaveLoadingView;


/**
 * Created by Feroz on 14-12-2016.
 */

public class CMSAssessmentFragment extends Fragment {
    public final static String ASSESSMENT_ID = "ASSESSMENT_ID";
    public static AssessmentLockableViewPager assessmentLockableViewPager;
    public static ArrayList<Entry> question_map, question_time;

    private int assessment_id;
    private CMSAssessment cmsAssessment;
    private CMSAssessmentResult cmsAssessmentResult;
    private AssessmentDataHandler assessmentDataHandler;
    private AssessmentStatusHandler assessmentStatusHandler;
    private ViewpagerAdapter viewpagerAdapter;


    private WaveLoadingView waveLoadingView;
    private ProgressBar prograss_bar;
    private RelativeLayout main_layout;
    private TextView number_of_ques, progress_text, question_timer_text, question_label;
    private Toast mToastToShow;

    private long start_time, end_time, question_start = 0, last_questionTimer;
    private CountDownTimer countDownTimer, questionTimer;
    private int delay = 120000, progress_status = 0, question_progress_time = 0;

    private TreeMap<Integer, Long> questionTimerData;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.new_cms_assessment_fragment, container, false);
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        main_layout = (RelativeLayout) view.findViewById(R.id.main_layout);
        waveLoadingView = (WaveLoadingView) view.findViewById(R.id.waveLoadingView);
        number_of_ques = (TextView) view.findViewById(R.id.number_of_ques);
        progress_text = (TextView) view.findViewById(R.id.progress_text);
        question_label = (TextView) view.findViewById(R.id.question_label);
        question_timer_text = (TextView) view.findViewById(R.id.question_timer_text);
        prograss_bar = (ProgressBar) view.findViewById(R.id.prograss_bar);
        mToastToShow = Toast.makeText(view.getContext(), "Hurry Up.!\n1 Minute left to submit assessment", Toast.LENGTH_LONG);

        assessmentLockableViewPager = (AssessmentLockableViewPager) view.findViewById(R.id.assessment_viewpager);
        assessmentDataHandler = new AssessmentDataHandler(getContext());
        assessmentStatusHandler = new AssessmentStatusHandler(getContext());
        cmsAssessmentResult = new CMSAssessmentResult();
        question_map = new ArrayList<>();
        question_time = new ArrayList<>();

        if (getArguments() != null) {
            if (getArguments().getString(ASSESSMENT_ID) != null) {
                assessment_id = Integer.parseInt(getArguments().getString(ASSESSMENT_ID));
                Log.v("Talentify", "Assessment Id---->" + assessment_id);
            }
        }


        assessmentLockableViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateslidePointerText();
                try {
                    CMSQuestion cmsQuestion = cmsAssessment.getQuestions().get(assessmentLockableViewPager.getCurrentItem());
                    if (questionTimerData.get(cmsQuestion.getId()) != null && questionTimerData.get(cmsQuestion.getId()) != 0) {
                        setUpQuestionTimer(questionTimerData.get(cmsQuestion.getId()));
                    } else if (questionTimer != null) {
                        questionTimer.cancel();
                        questionTimer = null;
                    }
                } catch (Exception e) {
                    if (questionTimer != null) {
                        questionTimer.cancel();
                        questionTimer = null;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        assessmentLockableViewPager.setSwipeLocked(true);

        cmsAssessmentResult.setAssessment_id(assessment_id + "");
        cmsAssessmentResult.setUser_id(SingletonStudent.getInstance().getStudent().getId() + "");

        try {
            Cursor c = assessmentDataHandler.getData(assessment_id);
            if (c.moveToFirst()) {
                setupOfflineAssement(c.getString(1));
            } else {
                fetchAssessmentFromServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fetchAssessmentFromServer();
        }

        return view;
    }

    private void setupOfflineAssement(String assessment_string) {
        
       /* StringReader reader = new StringReader(assessment_string);
        Serializer serializer = new Persister();*/
        try {

            Gson gnson = new Gson();
            cmsAssessment = gnson.fromJson(assessment_string, CMSAssessment.class);

       /* StringReader reader = new StringReader(assessment_string);
        Serializer serializer = new Persister();
        try {
            cmsAssessment = serializer.read(CMSAssessment.class, reader);*/
            viewpagerAdapter = new ViewpagerAdapter(getChildFragmentManager(), cmsAssessment);
            assessmentLockableViewPager.setAdapter(viewpagerAdapter);
            delay = cmsAssessment.getAssessmentDurationMinutes() * 60000;
            start_time = System.currentTimeMillis();

            //update the slide pointer.
            setupOfflineAssessmentSlide();
            createQuestionTimerValues();
            setupObject();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupOfflineAssessmentSlide() {
        try {
            Cursor c = assessmentStatusHandler.getData(assessment_id);
            if (c.moveToFirst()) {
                if (c.getString(2).equalsIgnoreCase("INCOMPLETED")) {
                    Serializer serializer = new Persister();
                    serializer.read(cmsAssessmentResult, c.getString(1));
                    int last_pointer = Integer.parseInt(c.getString(3));
                    delay = (cmsAssessment.getAssessmentDurationMinutes() * 60000) - Integer.parseInt(cmsAssessmentResult.getTotal_time());
                    assessmentLockableViewPager.setCurrentItem(last_pointer);

                    for (Entry entry : cmsAssessmentResult.getQuestion_map()) {
                        question_map.add(entry);
                    }
                    for (Entry entry : cmsAssessmentResult.getQuestion_time()) {
                        question_time.add(entry);
                    }
                    start_time = System.currentTimeMillis() - (Long.parseLong(cmsAssessmentResult.getTotal_time()));
                    last_questionTimer = Long.parseLong(c.getString(4));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchAssessmentFromServer() {
        new FetchAssessmentFromServer(getContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, assessment_id + "");
    }

    public void setupObject() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            progress_status = 0;
            prograss_bar.setProgress(0);
        }
        updateslidePointerText();
        progress_status = 0;
        prograss_bar.setMax(delay / 1000);
        countDownTimer = new CountDownTimer(delay, 1000) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {
                try {
                    long min = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                    long sec = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished));
                    String timerString = "00:00", minString = "" + min, secString = "" + sec;
                    if (min < 10) {
                        minString = "0" + min;
                    }
                    if (sec < 10) {
                        secString = "0" + sec;
                    }
                    timerString = minString + ":" + secString;
                    progress_text.setText(timerString);

                    if (min == 1 && sec == 0) {
                        mToastToShow.show();
                    }
                    prograss_bar.setProgress(progress_status++);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onFinish() {
                try {
                    progress_text.setText("00:00");
                    prograss_bar.setProgress(0);
                    progress_status = 0;

                    for (int i = assessmentLockableViewPager.getCurrentItem(); i < assessmentLockableViewPager.getAdapter().getCount() - 1; i++) {
                        updateCmsAssesmentResult(true);
                    }

                    //send to next fragment and submit data.
                    if (getActivity() != null)
                        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, new NextFragment()).commit();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        //start question Timer
        try {
            CMSQuestion cmsQuestion = cmsAssessment.getQuestions().get(assessmentLockableViewPager.getCurrentItem());
            if (questionTimerData.get(cmsQuestion.getId()) != null && questionTimerData.get(cmsQuestion.getId()) != 0) {
                setUpQuestionTimer(questionTimerData.get(cmsQuestion.getId()));
            } else if (questionTimer != null) {
                questionTimer.cancel();
                questionTimer = null;
            }
        } catch (Exception e) {
            if (questionTimer != null) {
                questionTimer.cancel();
                questionTimer = null;
            }
        }


        //visible toolbar
        main_layout.setVisibility(View.VISIBLE);
        waveLoadingView.setVisibility(View.VISIBLE);
    }

    private void setUpQuestionTimer(final long questionDelay) {
        question_progress_time = ((int) questionDelay / 1000);
        question_start = System.currentTimeMillis();
        waveLoadingView.setProgressValue(0);
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
        Log.v("Talentify", "question Timer---->" + question_progress_time);
        questionTimer = new CountDownTimer(questionDelay, 1000) {
            public void onTick(long millisUntilFinished) {
                try {
                    long min = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                    long sec = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished));

                    String timerString = "00:00", minString = "" + min, secString = "" + sec;
                    if (min < 10) {
                        minString = "0" + min;
                    }
                    if (sec < 10) {
                        secString = "0" + sec;
                    }
                    if (minString.equalsIgnoreCase("00")) {
                        timerString = secString + " sec left";
                    } else {
                        timerString = minString + " min " + secString + " sec left";
                    }

                    question_timer_text.setText(timerString);

                    if (min == 0 && sec == 10) {
                        Toast.makeText(getContext(), "Hurry Up.!\n" + "10 Second is left for Answer this question", Toast.LENGTH_SHORT).show();
                    }

                    int progress = ((int) (((millisUntilFinished / 1000) * 100) / question_progress_time));
                    waveLoadingView.setProgressValue(progress);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onFinish() {
                try {
                    updateCmsAssesmentResult(true);
                    if (assessmentLockableViewPager.getCurrentItem() == assessmentLockableViewPager.getAdapter().getCount() - 1) {
                        if (getActivity() != null)
                            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, new NextFragment()).commit();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    public static void nextViewpager(String key, String answer, String time) {
        if (assessmentLockableViewPager.getCurrentItem() != (assessmentLockableViewPager.getAdapter().getCount() - 1)) {
            assessmentLockableViewPager.setCurrentItem(assessmentLockableViewPager.getCurrentItem() + 1);
            question_map.add(new Entry(key, answer));
            question_time.add(new Entry(key, time));
        }
    }

    public void updateslidePointerText() {
        try {
            if (assessmentLockableViewPager.getCurrentItem() == assessmentLockableViewPager.getAdapter().getCount() - 1) {
                number_of_ques.setText("");
                main_layout.setVisibility(View.GONE);
                waveLoadingView.setVisibility(View.GONE);
            } else {
                number_of_ques.setText((assessmentLockableViewPager.getCurrentItem() + 1) + "/" + (assessmentLockableViewPager.getAdapter().getCount() - 1));
                main_layout.setVisibility(View.VISIBLE);
                waveLoadingView.setVisibility(View.VISIBLE);

                try {
                    if (cmsAssessment != null) {
                        String template = cmsAssessment.getQuestions().get(assessmentLockableViewPager.getCurrentItem()).getTemplate();
                        String label = "";
                        if (template.equalsIgnoreCase("2") || template.equalsIgnoreCase("4")) {
                            label = "Multiple Choice";
                        } else {
                            label = "Single Choice";
                        }
                        question_label.setText(label);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CMSAssessmentResult getAllAssmentResult() {
        cmsAssessmentResult.setQuestion_map(question_map);
        cmsAssessmentResult.setQuestion_time(question_time);
        end_time = System.currentTimeMillis();
        cmsAssessmentResult.setTotal_time((end_time - start_time) + "");
        return cmsAssessmentResult;
    }


    private void updateCmsAssesmentResult(boolean flag) {
        try {
            final String key, value, time;
            long start_time_of_question;

            if (viewpagerAdapter.getItem(assessmentLockableViewPager.getCurrentItem()) instanceof MultipleOptionSingleChoice) {
                key = ((TextView) ((MultipleOptionSingleChoice) viewpagerAdapter.instantiateItem(assessmentLockableViewPager, assessmentLockableViewPager.getCurrentItem())).getView().findViewById(R.id.hidden_key)).getText().toString();
                value = ((TextView) ((MultipleOptionSingleChoice) viewpagerAdapter.instantiateItem(assessmentLockableViewPager, assessmentLockableViewPager.getCurrentItem())).getView().findViewById(R.id.hidden_value)).getText().toString();
                start_time_of_question = Long.parseLong(((TextView) ((MultipleOptionSingleChoice) viewpagerAdapter.instantiateItem(assessmentLockableViewPager, assessmentLockableViewPager.getCurrentItem())).getView().findViewById(R.id.hidden_time)).getText().toString());

            } else {
                key = ((TextView) ((MultipleOptionMultipleChoice) viewpagerAdapter.instantiateItem(assessmentLockableViewPager, assessmentLockableViewPager.getCurrentItem())).getView().findViewById(R.id.hidden_key)).getText().toString();
                value = ((TextView) ((MultipleOptionMultipleChoice) viewpagerAdapter.instantiateItem(assessmentLockableViewPager, assessmentLockableViewPager.getCurrentItem())).getView().findViewById(R.id.hidden_value)).getText().toString();
                start_time_of_question = Long.parseLong(((TextView) ((MultipleOptionMultipleChoice) viewpagerAdapter.instantiateItem(assessmentLockableViewPager, assessmentLockableViewPager.getCurrentItem())).getView().findViewById(R.id.hidden_time)).getText().toString());
            }
            if (assessmentLockableViewPager != null && assessmentLockableViewPager.getCurrentItem() == 0) {
                start_time_of_question = start_time;
            }

            time = ((System.currentTimeMillis() - start_time_of_question) / 1000) + "";

            if (value != null && !value.equalsIgnoreCase("")) {
                System.out.println("-----------selected Ansewer-----> " + value);
                nextViewpager(key, value, time);
            } else if (flag) {
                nextViewpager(key, -1 + "", time);
            } else {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.content_for_skip_title)
                        .content(R.string.content_for_skip)
                        .positiveText("Yes")
                        .negativeText("No")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                nextViewpager(key, -1 + "", time);
                                dialog.dismiss();
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class FetchAssessmentFromServer extends AsyncTask<String, Integer, String> {

        private Context context;
        boolean response_success = true;

        public FetchAssessmentFromServer(Context context) {
            this.context = context;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... params) {
            String xml_object = null;
            try {
                HttpClient httpclient = new DefaultHttpClient();
                String BASE_URL = context.getResources().getString(R.string.server_ip) + "/get_offline_assessment?content_type=JSON&assessment_id=" + params[0];
                Log.v("Talentify", "ASSESSMENT_URL " + BASE_URL);

                int timeout = 80; // seconds
                HttpParams httpParams = httpclient.getParams();
                httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout * 1000);
                httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout * 1000);
                HttpPost httppost = new HttpPost(BASE_URL);
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                xml_object = EntityUtils.toString(entity, "UTF-8");
                assessmentDataHandler.saveContent(params[0], xml_object);
            } catch (Exception e) {
                response_success = false;
                e.printStackTrace();
            }
            return xml_object;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.equalsIgnoreCase("")) {
                setupOfflineAssement(result);
            }
        }
    }


    private void createQuestionTimerValues() {
        try {
            questionTimerData = new TreeMap<>();
            if (cmsAssessment != null && cmsAssessment.getAssessmentDurationMinutes() != null && cmsAssessment.getQuestions() != null && cmsAssessment.getQuestions().size() > 0) {
                List<CMSQuestion> cmsQuestions = cmsAssessment.getQuestions();

                for (CMSQuestion cmsQuestion : cmsQuestions) {
                    if (cmsQuestion != null && cmsQuestion.getDurationInSec() != null && cmsQuestion.getDurationInSec() != 0) {
                        long questionDuration = cmsQuestion.getDurationInSec() * 1000;
                        try {
                            if (cmsQuestion.getId() == cmsAssessment.getQuestions().get(assessmentLockableViewPager.getCurrentItem()).getId())
                                questionDuration = questionDuration - last_questionTimer;
                        } catch (Exception e) {
                        }
                        questionTimerData.put(cmsQuestion.getId(), questionDuration);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        try {
            if (getAllAssmentResult() != null) {
                if (assessmentLockableViewPager.getCurrentItem() == assessmentLockableViewPager.getAdapter().getCount() - 1) {
                    AssessmentStatusHandler assessmentStatusHandler = new AssessmentStatusHandler(getContext());
                    Serializer serializer = new Persister();
                    StringWriter stringWriter = new StringWriter();
                    serializer.write(cmsAssessmentResult, stringWriter);
                    String value = stringWriter.toString();
                    assessmentStatusHandler.saveContent(assessment_id + "", value, "COMPLETED", (assessmentLockableViewPager.getCurrentItem()) + "", (System.currentTimeMillis() - question_start) + "");

                    new SubmitAssessmentAsyncTask(getContext().getApplicationContext(), cmsAssessmentResult, assessmentLockableViewPager.getCurrentItem()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    AssessmentStatusHandler assessmentStatusHandler = new AssessmentStatusHandler(getContext());
                    Serializer serializer = new Persister();
                    StringWriter stringWriter = new StringWriter();
                    serializer.write(cmsAssessmentResult, stringWriter);
                    String value = stringWriter.toString();
                    System.out.println("value---------------->\n" + value);
                    assessmentStatusHandler.saveContent(assessment_id + "", value, "INCOMPLETED", (assessmentLockableViewPager.getCurrentItem()) + "", (System.currentTimeMillis() - question_start) + "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
    }
}