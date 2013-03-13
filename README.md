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

# The information of registered programs

It manages the information of programs. 
The word "program" imply either a *library* which is used by an
executable program or an *executable* which you can execute as a 
standalone. 
Files as entities of programs are transfered separately.

The informations for a program consists of

* a name of program
* a list of name spaces which the program implements
* if it's an executable, name space which implements an entry point
* if it's an executable, if you want it to be executed automatically or
you execute it manually
* if it's an executable, a timing to execute

# The files

Source code of programs, compiled binary,
resources which is used by programs and html files accessed as web pages
via the http interface are able to be deployed arbitral path under
an ldb.
Emjed supports the sending, receiving, deleteing, renaming the name and
moving between directories of files.

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

## LDB general

command      | arguments   | meaning
-------------|-------------|-----
`ldb/pwd`    | | gets the current directory of LDB
`ldb/cd`     | <i>path</i> | changes the current directory of LDB and load `conf.json` and `prog.json` under the new directory.
<i>path</i> is the valid relative or absolute path on target OS
`ldb/load`   | | (re)loads `conf.json` and `prog.json` onto memory
`ldb/save`   | | saves contents on memory to `conf.json` and `prog.json` files
`ldb/export` | | gets contents of `conf.json`, `prog.json` and files under `files` as a zipped file (not implemented yet)
`ldb/import` | | sends the zipped file consisting of `conf.json`, `prog json` and files under `files` to the target (not implemented yet)

## The information of registered program

command          | arguments      | meaning
-----------------|----------------|-----
`ldb/register`   | <i>name-kw</i> <i>map</i> | registers a program assumed that the file which is the entity of the program have been transfered.  Give the name of program as <i>name-kw</i>, <i>map</i> is a map includes a vector of name spaces the program as `:name-spaces`, a name space of an entry point of the program as a value of the key `:main`, a string "AUTO" or "MANUAL" as a value of the key `:execution` and a string "ONCE", "LOOP", "INTERVAL" as a value of the key `:execution`.  `:name-spaces` have not to include the name space which you specify in `:main`. (i.e. the information about the program which have only one entry point name space, have not to have `:name-spaces`. If the program is a library, it have not to have `:main` and `:timing`
`ldb/pload`      | <i>name-kw</i> | loads the namespaces which are specified in the registered program. Give the name of the program as <i>name-kw</i>.
`ldb/registered` |                | returns informations of the registered programs as a map.
`ldb/unregister` | <i>name-kw</i> | deletes the registration of the program specified by <i>name-kw</i>.
`ldb/build`      | <i>name-kw</i> | compiles the namespaces specified in the registration of the program specified by <i>name-kw</i>.
`ldb/exec-fn`    | <i>fqf</i> <i>args</i> ... | executes functions which is specified by a fully qualified function name as <i>fqf</i> with arbitrary number of given aruguments as <i>args</i>. This is only for debug.
`ldb/exec`       | <i>name-kw</i> | executes the function `-main` which is specified as `:main` of information of a registered program with the timing specified as `:timing` of that and returns the ID of process.  "Process" is the word representing an entity of runnning program. 
登録されたプログラムの名称をキーワードとして与え,  `:main` に指定された名前空間の `-main` 関数を, `:timing` に指定された方法で実行します. 実行したプロセスの ID を返します. *プロセス* とは実行中のプログラムの実態を指す言葉で, 同じプログラムを複数同時に起動した場合別のプロセスとなります.
`ldb/kill`       | <i>pid</i>     | プロセス ID を指定して実行中のプログラムを停止します.
`ldb/ps`         |                | プロセス一覧をマップで返します. キーがプロセス ID, 値はプロセスの状態を格納するマップです.

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

