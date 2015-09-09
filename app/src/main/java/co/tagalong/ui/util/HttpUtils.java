package co.tagalong.ui.util;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

/**
 * Created by piedt on 2/21/15.
 */
public class HttpUtils {

    public static int getStatusCode(HttpResponse response) {
        StatusLine status = response.getStatusLine();
        return status.getStatusCode();
    }
}
