package com.beneficial02.test.dic_dejizo_soap;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.ArrayList;

/**
 * http://dejizo.jp/dev/soap.html
 */
public class MainActivity extends Activity {
  static final String TAG = MainActivity.class.getSimpleName();

  private static final String SOAP_ACTION = "http://MyDictionary.jp/SOAPServiceV11/SearchDicItem";
  private static final String URL = "http://public.dejizo.jp/SoapServiceV11.asmx";
  private static final String NAME_SPACE = "http://MyDictionary.jp/SOAPServiceV11";
  private static final String METHOD_NAME = "SearchDicItem";

  private static final String AuthTicket = ""; //blank
  private static String targetWord;

  private static final String SortOrderID = ""; //blank
  private static final int ItemStartIndex = 0;
  private static final int ItemCount = 5;
  private static final int CompleteItemCount = 5;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Log.e(TAG, "onCreate: " + new AsyncFindDict().execute() );
  }

  private class AsyncFindDict extends AsyncTask<Void, Void, String> {
    @Override
    protected String doInBackground(Void... voids) {
      SoapObject request = new SoapObject(NAME_SPACE, METHOD_NAME);

      request.addProperty("AuthTicket", AuthTicket);

      SoapObject dicIdList = new SoapObject(NAME_SPACE, "DicIDList");
      dicIdList.addProperty(NAME_SPACE, "guid", "8a68bb8a-16ee-4b51-afaa-74c277bb600a");
      request.addProperty(NAME_SPACE, "DicIDList", dicIdList);

      SoapObject queryList = new SoapObject(NAME_SPACE, "QueryList");
      SoapObject query = new SoapObject(NAME_SPACE, "Query");

      /******** TO SEARCH ANOTHER WORD, CHANGE THIS WORD! *********/
      targetWord = "かた";

      query.addProperty(NAME_SPACE, "Words", targetWord);
      query.addProperty(NAME_SPACE, "ScopeID", "HEADWORD");
      query.addProperty(NAME_SPACE, "MatchOption", "EXACT");
      query.addProperty(NAME_SPACE, "MergeOption", "AND");
      queryList.addProperty(NAME_SPACE, "Query", query);
      request.addProperty(NAME_SPACE, "QueryList", queryList);

      SoapObject contentProfile = new SoapObject(NAME_SPACE, "ContentProfile");
      contentProfile.addProperty(NAME_SPACE, "FormatType", "XHTML");
      contentProfile.addProperty(NAME_SPACE, "ResourceOption", "URI");
      contentProfile.addProperty(NAME_SPACE, "CharsetOption", "UNICODE");
      request.addProperty(NAME_SPACE, "ContentProfile", contentProfile);

      request.addProperty(NAME_SPACE, "SortOrderID", SortOrderID);
      request.addProperty(NAME_SPACE, "ItemStartIndex", ItemStartIndex);
      request.addProperty(NAME_SPACE, "ItemCount", ItemCount);
      request.addProperty(NAME_SPACE, "CompleteItemCount", CompleteItemCount);

      return callDictAPI(URL, SOAP_ACTION, request);
    }

    private String callDictAPI(String strURL, String strSoapAction, SoapObject request) {
      try {
        StringBuffer result;
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        envelope.setAddAdornments(false);
        envelope.implicitTypes = true;

        HttpTransportSE ht = new HttpTransportSE(strURL);
        ht.debug = true;
        ht.call(strSoapAction, envelope);

        SoapObject response = (SoapObject) envelope.bodyIn;

        Log.e(TAG, "callDictAPI: response: " + response);

        String dictRawResult = response.getPropertyAsString(1);
        String substrUntilWord;
        String word = "";
        String substrAfterWord = "";
        ArrayList<String> dictResult = new ArrayList<>();
        boolean isKana = false;
        String kanji = "";

        if(targetWord.matches("^[\\u3040-\\u309F]+$") || targetWord.matches("^[\\u30A0-\\u30FF]+$")) {
          Log.e(TAG, "callDictAPI: it's KANA!!");
          isKana = true;
        }

        for (int i=0; i<((SoapObject)response.getProperty(1)).getPropertyCount(); i++) {
          if (isKana) {
            if (i == 0) {
              kanji = dictRawResult.substring(dictRawResult.indexOf("Title="));
              kanji = kanji.substring(kanji.indexOf("span=")+5, kanji.indexOf(";"));
            } else {
              Log.e(TAG, "callDictAPI: substrafterword::" + substrAfterWord);
              kanji = substrAfterWord.substring(substrAfterWord.indexOf("Title="));
              kanji = kanji.substring(kanji.indexOf("span=")+5, kanji.indexOf(";"));
            }
          }

          if (i==0) {
            substrUntilWord = dictRawResult.substring(dictRawResult.indexOf(")"));
          } else {
            substrUntilWord = substrAfterWord.substring(substrAfterWord.indexOf(") "));
          }

          word = substrUntilWord.substring(substrUntilWord.indexOf(")")+1, substrUntilWord.indexOf(";")).trim();
          substrAfterWord = substrUntilWord.substring(word.length()-1);

          if (word.contains("(") && word.contains(")")) {
            word = word.replaceAll("\\(.*?\\) ?", ""); // remove parentheses in 'word'
          }

          if (isKana) word = "["+kanji+"]" + " " + word;

          if (i==0) {
            dictResult.add(word);
          } else {
            if (!dictResult.get(dictResult.size()-1).equals(word)) {
              dictResult.add(word);
            }
          }
        }

        Log.e(TAG, "callDictAPI: RESULT!: " + dictResult);

        result = new StringBuffer(response.toString());
        return result.toString();

      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    protected void onPostExecute(String s) {
    }
  }

}