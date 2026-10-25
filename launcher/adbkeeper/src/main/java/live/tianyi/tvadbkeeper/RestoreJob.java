package live.tianyi.tvadbkeeper;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class RestoreJob extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        Restore.apply(this, "job-" + params.getJobId());
        return false;
    }
    @Override public boolean onStopJob(JobParameters params) { return false; }
}
