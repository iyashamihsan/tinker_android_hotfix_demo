package tinker.sample.android.servercom;

import retrofit2.Call;
import retrofit2.http.GET;
import tinker.sample.android.models.ConfigResponseObject;

public interface GetDataService {

    @GET("config")
    Call<ConfigResponseObject> getConfig();
}