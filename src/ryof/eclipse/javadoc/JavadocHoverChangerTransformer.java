package ryof.eclipse.javadoc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;

/**
 * Javadocホバー表示クラス書き換え用のトランスフォーマ
 * <br />
 * EclipseのJavadocホバー表示をソースより優先して指定したURLから取得するようにメソッドを書き換えます。
 * (クラスローダに読み込まれる直前のクラスファイルの書き換えを行います)
 * 
 * @author Ryo.F
 *
 */
public class JavadocHoverChangerTransformer implements ClassFileTransformer {
	
	/** 書き換え対象のクラス */
	private static final String TARGET_CLASS_PATH = "org/eclipse/jdt/internal/ui/text/javadoc/JavadocContentAccess2";
	/** 書き換え対象のメソッド名 */
	private static final String TARGET_METHOD = "getHTMLContent";
	/** 書き換え対象のメソッドのシグネチャ(Eclipse 3.4 〜 4.4) */
	private static final String TARGET_METHOD_DESC_3_4 = "(Lorg/eclipse/jdt/core/IMember;Z)Ljava/lang/String;";
	/** 書き換え対象のメソッドのシグネチャ(Eclipse 4.5) */
	private static final String TARGET_METHOD_DESC_4_5 = "(Lorg/eclipse/jdt/core/IJavaElement;Z)Ljava/lang/String;";
	
	/** javassistが管理するロード前のクラス定義バッファ */
	private final ClassPool classPool;
	private final Instrumentation inst;
	
	public JavadocHoverChangerTransformer(Instrumentation inst) {
		classPool = ClassPool.getDefault();
		this.inst = inst;
	}

	/**
	 * クラスローダに対してクラスファイルの読み込みが行われるたびに、その直前に呼ばれるリスナーイベント
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		//変換対象のクラスでない場合
		if (!className.equals(TARGET_CLASS_PATH)) {
			//クラス定義に変更が無い場合はnullとする
			return null;
		}
		
		System.out.println("クラス変換開始。");
		try {
			//クラスファイルのバイトコードからjavassistで扱える形式にする
			CtClass ctClass = makeClass(classfileBuffer);
			
			//クラス定義からクラス内に定義したフィールドやメソッド個々の定義を取得する
			for (CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
				//変換対象のメソッドであるかチェック
				if (isTargetMethod(behavior)) {
					return transformMethod(loader, behavior);
				}
			}
		} catch (Exception ex) {
			IllegalClassFormatException e = new IllegalClassFormatException();
			e.initCause(ex);
			throw e;
		}
		return null;
	}
	
	/**
	 * クラスファイルのバイトコードからjavassistで扱える形式にします。
	 * @param classfileBuffer クラスのバイトコード
	 * @return javassist APIが扱うクラス定義クラス
	 * @throws IOException 一般的なIOエラー
	 * @throws RuntimeException その他予期しないエラー
	 */
	private CtClass makeClass(byte[] classfileBuffer) throws IOException, RuntimeException {
		return classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
	}
	
	/**
	 * EclipseのJavadocホバーの制御を行うメソッドであるか確認します。
	 * @param behavior チェックするクラス内定義
	 * @return 変換対象のメソッドの場合true
	 */
	private boolean isTargetMethod(CtBehavior behavior) {
		if (!behavior.getName().equals(TARGET_METHOD)) {
			return false;
		}
		//変換対象メソッドと同名であってもオーバーロードされた別のメソッドの可能性があるのでシグネチャもチェックする
		final String desc = behavior.getMethodInfo().getDescriptor();
		//Eclipse 4.5
		if (desc.equals(TARGET_METHOD_DESC_4_5)) {
			return true;
		}
		//Eclipse 3.4〜4.4
		if (desc.equals(TARGET_METHOD_DESC_3_4)) {
			return true;
		}
		//上記以前の場合はクラスそのものが違うためサポート外とする
		return false;
	}
	
	/**
	 * メソッドの変換を行います。
	 * <br />
	 * 対象メソッド（{@link JavadocHoverChangerTransformer#TARGET_METHOD}）定義の先頭に、
	 * 関連付けしたURLからJavadocを取得する事を試す処理を挿入する。
	 * 取得出来た場合はそれを戻り値とし、取得出来なかった場合は元のメソッド処理を行うようにする。
	 * 
	 * @param loader 対象クラスのロードを行う直前のクラスローダ
	 * @param behavior 変換対象メソッド定義
	 * @return 返還後のメソッドを含むクラスのバイトコード
	 * @throws Exception
	 */
	private byte[] transformMethod(ClassLoader loader, CtBehavior behavior) throws Exception {
		try {
			System.out.println("メソッド変換開始。");
			
			//変換対象のメソッド定義には他のパッケージのクラス定義があるので、
			//クラスローダから定義を取得しておく
			classPool.appendClassPath(new LoaderClassPath(loader));
			
			//Javadocホバーの内容を出力するメソッドが実行されたら、
			//最初に関連付けされているURLからJavadocを取得するようにする。
			//(取得出来なかった場合は元のメソッド実装を実行する)
			behavior.insertBefore("{"
					+ "String s = $1.getAttachedJavadoc(null);"
					+ "if (s != null) return s;"
					//+ "if (s == null) $_ = $proceed($$);"
					+ "}");
			
			//変換対象メソッドの変換が終われば、クラスロードをフックする必要がないので、
			//リスニングを中止する
			inst.removeTransformer(this);
			
			final byte[] result = behavior.getDeclaringClass().toBytecode();
			System.out.println("メソッド変換完了しました。");
			
			//クラス定義に変更がある場合は変更後のバイトコードを返す
			return result;
		} catch(Exception ex) {
			System.out.println("メソッド変換に失敗しました。");
			ex.printStackTrace();
			throw ex;
		}
	}
}
