package live.tianyi.fixture;
import android.app.Activity;import android.os.Bundle;import android.view.Gravity;import android.widget.TextView;
public class StubActivity extends Activity { @Override public void onCreate(Bundle b){super.onCreate(b);TextView t=new TextView(this);t.setText("本地测试替身 · 不会播放视频或登录账号\n\n"+getPackageName()+"\n\n"+getIntent().getAction()+"\n\n"+(getIntent().getDataString()==null?"应用启动成功":getIntent().getDataString())+"\n\n按返回回到桌面");t.setGravity(Gravity.CENTER);t.setTextSize(24);t.setTextColor(-1);t.setBackgroundColor(0xff18202b);setContentView(t);} }
