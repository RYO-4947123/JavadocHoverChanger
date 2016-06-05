package ryof.eclipse.javadoc;

import java.lang.instrument.Instrumentation;

/**
 * エントリクラス
 * <br/>
 * Javaアプリケーション起動オプション「-javaagent」にこのクラスを含むjarを指定して実行する事で、
 * 元アプリケーションのmainメソッド（エントリポイント）実行より先に処理する事が出来ます。
 * 
 * @author Ryo.F
 *
 */
public class Main {

	/**
	 * エントリポイント
	 * @param agentArg
	 * @param inst
	 * @throws Throwable
	 */
	public static void premain(final String agentArg, final Instrumentation inst) throws Throwable {
		System.out.println("javadochoverchanger start.");
		inst.addTransformer(new JavadocHoverChangerTransformer(inst));
	}
}
