package assessment.android.istar.com.androidassessment.assessment_util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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

import java.io.StringReader;
import java.util.TreeMap;

import assessment.android.istar.com.androidassessment.R;
import assessment.android.istar.com.androidassessment.assessment_database.AssessmentDataHandler;
import assessment.android.istar.com.androidassessment.assessment_pojo.CMSAssessment;
import assessment.android.istar.com.androidassessment.assessment_pojo.CMSQuestion;
import assessment.android.istar.com.androidassessment.istarindia.complexobject.XMLLesson;

/**
 * Created by ajith on 13-12-2016.
 */

public class SaveAllAssessmentAsyncTask extends AsyncTask<String, Integer, String> {
    private Context context;
    private AssessmentDataHandler assessmentDataHandler;

    public SaveAllAssessmentAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    @Override
    protected String doInBackground(String... params) {
        try {
            Assessmentutil assessmentutil = new Assessmentutil(context);
            TreeMap<Integer, XMLLesson> integerXMLLessonTreeMap = assessmentutil.getAllAssessment();
            assessmentDataHandler = new AssessmentDataHandler(context);
            for (Integer integer : integerXMLLessonTreeMap.keySet()) {
                if (integerXMLLessonTreeMap.get(integer).getType().equalsIgnoreCase("ASSESSMENT")) {


                    int assessment_id = integerXMLLessonTreeMap.get(integer).getConcreteId();
                    System.out.println("--------------------AssesmentID---------------" + assessment_id);

                    HttpClient httpclient = new DefaultHttpClient();
                    String BASE_URL = context.getResources().getString(R.string.server_ip) + "/get_offline_assessment?content_type=JSON&assessment_id=" + assessment_id;
                    Log.v("Talentify", "BASE_URL " + BASE_URL);

                    int timeout = 20; // seconds
                    HttpParams httpParams = httpclient.getParams();
                    httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout * 1000);
                    httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout * 1000);

                    HttpPost httppost = new HttpPost(BASE_URL);

                    HttpResponse response = httpclient.execute(httppost);

                    HttpEntity entity = response.getEntity();
                    String xml_object = EntityUtils.toString(entity, "UTF-8");
                    try {
                        Gson gnson = new Gson();
                        CMSAssessment example = gnson.fromJson(xml_object, CMSAssessment.class);
                        System.out.println("example " + example.getAssessmentId());

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    assessmentDataHandler.saveContent(assessment_id + "", xml_object);


                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


        return "";
    }

    protected void onPostExecute(String result) {

    }
}
