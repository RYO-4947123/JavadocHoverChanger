# JavadocHoverChanger
EclipseのJavadocホバーをURL優先にする

このプログラムはEclipseのJavadocホバーに表示する内容をJavadocロケーションで指定したURLから優先的に取得する事を可能にします。

Eclipseの仕様では、jarファイル等のクラスライブラリにソースとJavadocロケーションいずれかを関連付けする事で、
そのクラスのJavadocをホバー表示する事が出来ますが、
ソースとロケーション両方を指定している場合は、ソースに書かれたJavadocを優先して表示します。
その順番を入れ替えます。

主に以下のケースで有用となります。
<ul>
<li>ソースコードはオリジナルを参照したいが、Javadocは日本語で表示したい</li>
</ul>

仕組みはPleiadesと同様にJavadocホバー内容出力メソッドを書き換えて優先順を入れ替えています。<br/>
（厳密にはPleiadesはそのメソッドの呼び出し元を書き換えていますが、
このプログラムはそのメソッドの内容自体を書き換えています。
やっていることはほぼ等価のはずです・・・）

Eclipseのバージョンは3.4〜4.5が対象です。<br/>
確認環境は4.5にバンドルしているSTSで行っていますので、STSでも問題ありません。<br/>

○設定手順
<ol>
<li>必要なファイルの置き方</li>
 Windowsの場合は、eclipse.exeがあるフォルダ、
 Macの場合は、pluginsフォルダと同階層に
 「javadochoverchanger」という名前でフォルダを作成します。<br/>
 その中にこのリポジトリの「lib」フォルダと「javadochoverchanger.jar」を入れます。
<li>iniファイルの修正</li>
 「eclipse.ini」ファイルの修正を行います。<br/>
 Windowsの場合は、eclipse.exeが有るフォルダの「eclipse.ini」を開いて以下の文を追加します。<br/>
 「-javaagent:javadochoverchanger/javadochoverchanger.jar」<br/>
 Macの場合は、4.5とそれ以前でフォルダ構成が異なっています。<br/>
 ・4.4以前<br/>
 　Eclipse.appのパッケージの内容を開いてeclipse実行ファイルが有るフォルダの「eclipse.ini」を開いて以下の文を追加します。<br/>
 「-javaagent:../../../javadochoverchanger/javadochoverchanger.jar」<br/>
 ・4.5以降<br/>
 　Eclipse.appのパッケージの内容を開いてpluginsフォルダが有るフォルダの「eclipse.ini」を開いて以下の文を追加します。<br/>
 「-javaagent:../Eclipse/javadochoverchanger/javadochoverchanger.jar」<br/>
 (STSの場合は、「eclipse.ini」を「STS.ini」に読み替えて下さい。)
</ol>

※当方はこのプログラムによる一切の責任を負いません。全て自己責任でお願いします。
