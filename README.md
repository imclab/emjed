# What is this

Emjed is a software controlling programs for embedded computers
written in Clojure.

Emjed requires an implementation of JRE 1.6 or higher installed
and configured.

Emjed runs on a JVM on a target computer, and gives controlling
of programs to remote computers via TCP connection.
In this context, "controlling programs" means:
* Putting, getting and compiling of program source code
* Putting and getting of binary programs
* Registration, unregistration and deletion of programs
* Specifing execution timings of the registered programs
* Starting and stopping registered programs
* Viewing and editing configuration items for programs
* Putting and getting resource files for programs

Emjed currently supports programs written only in Clojure.
Independent of the fact that emjed are written in Clojure,
the framework it gives accepts other languages.
Supports for other languages may be added near future.

Emjed uses a directory as its local database (mentioned as LDB
below).
LDB consists of
* The configuration: configuration items for programs
* The information of registered programs: information about namespaces,
execution timing and etc.
* The fils: souce code, compiled binary of programs and resource
files for programs

Emjed supplies two interfaces to access the LDB.
They are the library interface and the network interface.
The network interface works via telnet and http.
It uses JSON as its data format.
So it's easy to viewing and editting of configuration items
to which programs read and write.
A sample web pages to viewing and editting them. (under creating)
Try to access

    http://<host running emjed>:8080/tree.html

with your web browser.

The configuration and the information of registered programs are
loaded from files with particular names in the LDB when emjed executed
and when required and are kept on memory.
When required, they are saved to the original files.

# How to install

The built JAR file is in
[downloads](https://github.com/kohyama/emjed/downloads). 
Please download the latest emjed-x.x-standalone.jar and copy it to
your target computer via arbitrary way.

When you build from source,
do

    $ git clone git://github.com/kohyama/emjed.git
    $ cd emjed/
    $ lein uberjar

on a computer with
[git](http://git-scm.com/) and
[leiningen](https://github.com/technomancy/leiningen/) installed,
and emjed-x.x-standalone.jar will be built.

Note that `$` should be replaced with a prompt of command interface
of your computer,
and `x.x` should be replaced with the version of emjed you downloaded.

Please copy
emjed-x.x-standalone.jar to your target computer via arbitrary way.

# Usage

Do

    $ java -jar emjed-x.x-standalone.jar [dir]

on a computer with JRE installed, and you can manage programs on it
via TCP connections.

# The configuration

The configuration is a on-memory database to save configuration items
for programs and temporary data.
On memory, it is a map of Clojure.
You can freely access the map with the library interface.
(Sorry I'm on translating below from Japanese to English)

A file `conf.json` is to load configuration from or save configuration to
and it's described in JSON format as the extension implies.

JSON format is also used as a data format when users access
the configuration via the network interface

Note that keywords of Clojure are used as keys in the library interface,
but strings are used as keys in JSON being used in files or the
network interface.

When the configuration is 

    {:foo "Hello"
     :bar "World"
     :baz {:qux 3.14}}

in Clojure, you can get `3.14` with doing `ldb/get [:baz :qux]` with
the library interface, or `get :baz:qux` with the network interface.
And you will get

    {"foo": "Hello",
     "bar": "World",
     "baz": {"qux": 3.14}}

when doing `getrec :` with the network interface.

Execution of saving command saves the configuration into `conf.json`
file (automatically created if it doesn't exist) in the same format
as the return of `getrec :`.

# 登録プログラム情報

プログラムの情報を管理します.  
プログラムは, 他のプログラムから利用されることのみを前提とする *ライブラリ*
と, 単体のプログラムとして実行可能な *実行可能プログラム* の両方を指します.  
プログラムの実態となるバイナリやソースコードは別途ファイルとして送受信
します. **(要検討)**  

プログラムの情報は
* プログラムの名称
* プログラムが実装する名前空間のリスト
* 実行可能プログラムの場合, エントリポイント関数を実装する名前空間
* 実行可能プログラムの場合, 起動時に自動実行するか, 手動実行するかの情報
* 実行可能プログラムの場合, 実行タイミングの指定

から成ります.

# ファイル群

プログラムのソースコード, コンパイル済みバイナリ,
プログラムからアクセスされるリソース.
http インターフェイスで WEB ページとしてアクセスされる HTML ファイル.
などを LDB 以下の files というディレクトリの下に自由に配置します.  
ファイルの送信, 取得, 削除, ディレクトリや名称の変更などの操作が提供されます.

# Library Interface

## Abstraction

The library interface provides for programs to access the LDB.
To use the interface, load the `emjed.ldb` namespace with an
alias `ldb`.

    (require '[emjed.ldb :as ldb])

Arguments and return values of the commands are clojure literals.  
`kv` refers a vector of keywords like `[:foo :bar]`.  
`val` refers a string, a numerical, a boolean, a map or a vector.  
A path of a return value from `lcd/pwd` or an argument of `lcd/cd`
is a current directory of the LDB
in format of an absolute or a relative path in your OS's file system.  
Path arguments for commands begging with the letter 'f' are absolute
paths with assumed the current directory of LDB as the root.

## LDB 全般

命令 | 引数 | 内容
---------|------------|-----
`ldb/pwd` |             | 現在の LDB のカレントディレクトリを取得します.
`ldb/cd`  | path        | 現在の LDB のカレントディレクトリを変更し, `conf.json` と `prog.json` の内容をメモリ上にロードします. path は稼動している OS に於いて正しい, 相対パスまたは絶対パスです.
`ldb/load` | | LDB 以下の conf.json ファイルと prog.json ファイルの内容を, それぞれ, コンフィギュレーションおよび登録プログラム情報としてメモリ中に(再)ロードします.
`ldb/save` | | メモリ中のコンフィギュレーションと登録プログラム情報を LDB 以下の conf.json ファイルと prog.json ファイルとして保存します.
`ldb/export` | | conf.json, prog.json 及び files 以下のファイルを zip 圧縮して単一のファイルとして取得します. (未実装)
`ldb/import` | | conf.json, prog.json 及び files 以下のファイルを格納した zip 圧縮された単一のファイルを送信しロードします. (未実装)

## 登録プログラム情報

命令 | 引数 | 内容
---------|------------|-----
`ldb/register` | name-kw map | プログラムを登録します. 実態となるファイルは別途転送済みとします. name-kw にプログラムの名称をキーワードとして与え, map は `:name-spaces` に名前空間を文字列表現したもののベクタを, `:main` に実行可能プログラムとして実行する場合にエントリポイントとなる名前空間を文字列表現したもの, `:execution` に自動実行させたいか手動実行したいかに応じて "AUTO" または "MANUAL" を, `:timing` に `"ONCE"`, `"LOOP"` または, 後ほど規定する文字列表現された周期実行の実行タイミングを記載した文字列を格納したマップです. `:main` に記載した名前空間は `:name-spaces` に記載する必要はありません. 従ってエントリポイントとなる名前空間一つだけからなる実行可能プログラムは, `:name-spaces` を省略できます. またライブラリの場合は, `:main` 及び `:timing` を省略できます.
`ldb/pload`      | name-kw | 登録されたプログラムの名称をキーワードとして与え, そのプログラムの登録に一覧された名前空間をロードします.
`ldb/registered` |         | 登録されたプログラムの一覧をマップで返します.
`ldb/unregister` | name-kw | 登録されたプログラムの名称をキーワードとして与え, 登録を削除します.
`ldb/build`      | name-kw | 登録されたプログラムの名称をキーワードとして与え, そのプログラムの登録に一覧された名前空間をコンパイルします.
`ldb/exec-fn`    | fqf args ... | 名前空間で完全修飾された関数名を文字列として与え, 任意の型の任意の数の引数を指定して関数を実行します. デバッグ用です.
`ldb/exec`       | name-kw | 登録されたプログラムの名称をキーワードとして与え,  `:main` に指定された名前空間の `-main` 関数を, `:timing` に指定された方法で実行します. 実行したプロセスの ID を返します. *プロセス* とは実行中のプログラムの実態を指す言葉で, 同じプログラムを複数同時に起動した場合別のプロセスとなります.
`ldb/kill`       | pid     | プロセス ID を指定して実行中のプログラムを停止します.
`ldb/ps`         |         | プロセス一覧をマップで返します. キーがプロセス ID, 値はプロセスの状態を格納するマップです.

## コンフィギュレーション

命令 | 引数 | 内容
---------|------------|-----
`ldb/get`    | kv      | コンフィギュレーションの kv に指定されたノードの内容を取得します. 対象ノードが文字列, 数値, 真偽値の場合は値そのもの, マップの場合は含まれるキーの一覧をベクタとして返します. シーケンスの場合の挙動は不定です.
`ldb/getrec` | kv      | コンフィギュレーションの kv に指定されたノードの内容を取得します. マップやシーケンスの場合も全て下位の内容を含みます.
`ldb/set`    | kv val  | コンフィギュレーションの kv ノードの内容を val に指定された内容で上書きします. 対象ノードおよび先祖となるノードが存在しない場合は根まで遡って生成されます. ただし, 先祖ノードのいずれかがマップではない型で既に存在する場合例外を返します.
`ldb/del`    | kv      | kv に指定されたノード（およびその全ての子孫ノード）を削除します.
`ldb/rename` | skv dkv | skv に指定されたノードを dkv に指定されたノードに移動します.

## ファイル群

LDB として動作している現在のディレクトリ以下の `files` という名称のディレクトリ以下のファイル取扱ます. `src` ディレクトリは特別で, プログラムのソースコードを格納します.

命令 | 引数 | 内容
---------|------------|-----
`ldb/flist`   |         | ファイルのリストを取得します.
`ldb/fget`    | path    | path に指定されたファイルの内容をバイト配列として取得します.
`ldb/ftget`   | path    | path に指定されたファイルの内容を文字列として取得します.
`ldb/fput`    | path ba | path に指定されたファイルに, ba に指定されたバイト配列の内容を上書き保存します.
`ldb/ftput`   | path text | path に指定されたファイルに, text で指定された文字列の内容を上書き保存します.
`ldb/fdel`    | path    | path に指定されたファイルを削除します.
`ldb/frename` | spath dpath | spath に指定されたファイルを dpath に指定されたファイルに名称変更（ディレクトリの移動を含んで良い）します.

# ネットワークインターフェイス

`qk` (Qualified Keys) は, ルート `:` からのキーの連鎖を `:` で繋いで文字列として表現したもの. `:foo:bar` のように指定する.
`json-exp` はデータの JSON 表現.

## 全般

命令 | パラメータ | 内容
-----|------------|----
`version` | | バージョンを取得
`pwd`     | | LDB として設定されている現在のディレクトリを取得
`cd`      | path | path に指定されたディレクトリを LDB の現在のディレクトリとして設定し, conf.json および prog.json の内容をロード.
`load`    | | LDB として設定されている現在のディレクトリの conf.json と prog.json の内容を(再)ロード.
`save`    | | メモリ中のコンフィギュレーションと登録プログラム情報を conf.json および prog.json に保存.
`export`  | | conf.json, prog.json および files 以下のファイルを zip アーカイブとして取得. (未実装)
`import`  | | conf.json, prog.json および files 以下のファイル群を含む zip アーカイブを転送, LDBとして設定されている現在のディレクトリに展開, conf.json と prog.json の内容をコンフィギュレーション, 登録プログラム情報としてロードする.

## コンフィギュレーション

命令 | パラメータ | 内容
-----|------------|----
`get`    | qk      | qk に指定されたノードの内容を取得する. 文字列, 数値, 真偽値, null の場合は, その値の JSON 表現を, マップの場合は含まれるキーの一覧を JSON の文字列の配列として返す. 対象ノードが配列の場合の動作は不定.
`getrec` | qk      | qk に指定されたノードの内容を取得する.
`set`    | qk json-exp | qk に指定されたノードの内容を json-exp に指定された内容で上書き.
`del`    | qk      | qk に指定されたノード以下の全ての子孫ノードを削除
`rename` | sqk dqk | sqk

## 登録プログラム情報

命令 | パラメータ | 内容
-----|------------|----
`register` | map | プログラムの登録. (未実装)
`pload` | name | name に指定された登録プログラムの全ての名前空間をロード
`registered` | | 登録プログラム情報の取得
`unregister` | name | name に指定された登録プログラムを削除
`build`      | name | name に指定された登録プログラムの全ての名前空間をコンパイル
`exec-fn`    | fqf args ... | fqf で名前空間で完全修飾された関数名として与えられた関数を与えられた引数で実行. (デバッグ用. 不完全. 引数の型が全て文字列になってしまうがどうやって回避するか.)
`exec` | name | name に指定された登録プログラムを実行
`ps`   |      | プロセス一覧の取得
`kill` | pid  | pid に指定されたプロセスを停止

## ファイル群

命令 | パラメータ | 内容
-----|------------|----
`flist`   |             |
`fget`    | path        |     : returns size, "\r\n", binary, "\r\nOK\r\n"
`fput`    | path size   | : follow "\r\n", binary, "\r\n". returns "OK\r\n"
`fdel`    | path        |
`frename` | spath dpath |

# 利用例

ターゲットコンピュータ上に任意の名称のディレクトリ (ここでは `emjed.ldb`
とします）と, その下に `files` という名前のディレクトリ,
さらに下に `src` という名前のディレクトリを作成しておきます.  
**(これがネットワーク上からできないとおかしい)**  
emjed を起動します.

    java -jar emjed-x.x-standalone.jar

ターゲットコンピュータと TCP ポート 3000 で通信可能なコンピュータから
telnet でアクセスします.
(仮にターゲットコンピュータのホスト名を `tcomp` とします.)

    telnet tcomp 3000

ターゲットコンピュータ上で emjed を起動した時のカレントディレクトリから
の相対パスまたは絶対パスで指定し, 作成しておいたディレクトリを LDB とする
よう変更します.

    cd emjed.ldb
    OK

コンフィギュレーションに `foo-input` という項目を追加し,
内容を "Hello" とします.

    set :foo-input "Hello"
    OK

コンフィギュレーションの内容を確認します.

    getrec :
    {
      "foo-input" : "Hello"
    }

`:foo-input` の内容を取得し, 文字列を反転して, `:foo-output` に
設定するプログラムのソースコードを送信します.

    ftput src/foo.clj
    (ns foo (:gen-class))
    (require '[emjed.ldb :as ldb])
    (defn -main []
      (ldb/set [:foo-output]
        (apply str (reverse (ldb/get [:foo-input])))))
    ^D
    OK

ソースコードの内容を確認します.

    ftget src/foo.clj
    (ns foo (:gen-class))
    (require '[emjed.ldb :as ldb])
    (defn -main []
      (ldb/set [:foo-output]
        (apply str (reverse (ldb/get [:foo-input])))))
    
名前空間 `foo` をエントリポイントとしてプログラム `bar` を登録します.
(名前空間を指定しているか, 登録した名称を指定しているかの区別のために,
わざと別の名称にしていますが, 実際は関連した名称を利用します.)

    register bar {"main": "foo", "timing": "ONCE"}
    OK

登録されているプログラムの一覧を確認します.

    registered
    {
      "bar" : {
        "main" : "foo",
        "timing" : "ONCE"
      }
    }

`bar` が必要とする名前空間のソースコード (今の場合 `foo` 名前空間を
構成する `foo.clj` ファイル.) をコンパイルします.

    build bar
    OK

コンパイルした場合は自動でロードされますが,
コンパイル済みファイルを転送した場合, `pload` でロードする必要があります.
登録済みの情報が保存されている場合は,
emjed 起動時に自動でロードします.
`bar` を実行します.

    exec bar
    0

プロセス番号 (今の場合は 0) が返りました.
コンフィギュレーションの内容を確認します.

    getrec :
    {
      "foo-output" : "olleH",
      "foo-input" : "Hello"
    }

