# Pass

[![bb compatible](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://babashka.org)

A command line password/ secrets store.

The goal of Pass is to securely and locally store passwords or other secrets using best practices encryption techniques.


Pass is based on [stow](https://github.com/judepayne/stow) which encrypts information and stores it in a local sqlite database file. Put the file on your Dropbox/ similar and you have a password store that works across your computers.

Pass is written in Clojure (the babashka dialect), but the intended user is someone who's comfortable with the command line, but doesn't need to know anything about Clojure. Pass can store data using Clojure's elegant notation for structured data; maps, lists and vectors. I'll explain these in the tutorial below.


## Installation

**prerequisites**

[Babashka](https://github.com/babashka/babashka) should be installed.


### Installation with [bbin](https://github.com/babashka/bbin)


    bbin install https://github.com/judepayne/pass.git

then, run:

    pass stow.db

will prompt for a master password and create a sqlite database file called `stow.db` in the current directory. The database file can be called whatever you want.


### Manual installation


    git clone https://github.com/judepayne/pass.git

    cd pass

    ./pass.clj stow.db

(I suggest adding the path to path.clj to your PATH for regular use.)

will prompt for a master password and create a sqlite database file called stow.db in the current directory.


Of course, the database file doesn't need to be called 'stow.db' and in the local directory. You can specify the name of the file and locate it where you want!

Oh, please take good care of your main password. It's a one time deal.


## The pass console

Once you've initialized a new encrypted database or have authenticated against an existing one, you'll be in the Pass console and will see:

    pass>

This is where you enter commands to interact with Pass. Before the tutorial on using pass, a brief notes about the features of the console itself.

Using the up and down arrow keys will cycle you through previous commands.

You can use `tab` to auto-complete pass commands.

To quit the console, type 'q' or 'quit' or ':q' or ':quit' followed by `Enter`.



## Tutorial


All pass commands begin with a `:`. Type `:help` <enter> in the Pass console.


    pass> :help
    Welcome to the pass app's help.
    -------------------------
    Available commands:
    :add-node
    :add-nodes
    :delete-node
    :delete-parent
    :export
    :fmt
    :gen
    :get-all-nodes
    :get-default-parent
    :get-node
    :get-nodes-by-key
    :get-nodes-by-parent
    :help
    :import
    :list
    :list-parents
    :previous-versions
    :restore
    :set-default-parent
    :update-node
    :update-node-with
    For help on a command, type ':help :command-name'

    Additional help topics:
    :syntax
    For help on a topic, type ':help :topic-name'

Let's add a `node`. A node in pass is your basic unit of data storage. It's comprised of a parent, a key and a value. Think of the parent as a group (of keys and values). More on this later in the section called [Structuring your data](#structuring).

    pass> :add-node facebook.com :pwd "KGn'F8f9z1<<Q[%AA8$"
    {:id 1, :parent "facebook.com"}


`{:id 1, parent "facebook.com"}` means that the node has been added to the database with an id of 1 under the parent (think 'group') "facebook.com".

For the rules on when to surround strings in double quotes, see [here](#doublequotes).

> Before we go on, you might have noticed that we used `:pwd` rather than just `pwd` to specify the name of the key. What's the meaning of that? Well, `pass` is written in the Clojure programming language and can use Clojure data structures (as well as strings) to hold your data. Whenever we come across something Clojure'y, I'll explain it.

> `:pwd` is a (Clojure) keyword. It's a primitive type (like a string) which means that it evaluates to itself. It's used here instead of a string to provide a bit of 'visual structure'. You don't have to use keywords (except for the commands themselves - they are all keywords). The string `pwd` would have been just fine.

Let's check the node we just added in the database.


    pass> :get-all-nodes
    [{:id 1,
      :parent
      "099f00aa324175308ede8a5976822034a83c0c01db77ebf329737b587f082dcc38cd516b96c81fa1e5d7acbe10fb5740",
      :key
      "099f00aa324175308ede8a5976822034a83c0c01db77ebf329737b587f082dcc38cd516b96c81fa1e5d7acbe10fb5740",
      :value
      "e7dd0e5e7795f95cb89f7599912747e886c8ea1aa1bb78dd0ae7c5abd8bc4cd1e95b43dfe23db55f9d5e9f0c499dce6b331c908845dc8b11ecefbd263688addd9d4083f2eb12d31e906a8237b505704e",
      :version 1,
      :created "2023-04-05 20:04:13",
      :modified "2023-04-05 20:04:13"}]

Not easy to steal your facebook password now is it? Pass encrypts the parent, key and value of each node. If someone did a hexdump of the database file, this is what they'd see.

But how to we get our password back when we need it?

    pass> :help :get-all-nodes
    :get-all-nodes
    -------------------------
    arglists:  ([& {:keys [decrypt?], :or {decrypt? false}}])

    Return all nodes. :decrypt? indicates whether the keys and values
    should be decrypted.

Ok, so we need to set `:decrypt?` to true..

    pass> :get-all-nodes :decrypt? true
    ({:version 1,
      :created "2023-04-08 05:49:45",
      :modified "2023-04-08 05:49:45",
      :id 1,
      :parent "facebook.com",
      :key :pwd,
      :value "KGn'F8f9z1<<Q[%AA8$"})

Let's add a `:user-id` to the facebook.com parent.

    pass> :add-node facebook.com :user-id "philippak@gmail.com"
    {:id 2, :parent "facebook.com"}


and inspect all the nodes with the parent set to facebook.com:

    pass> :get-nodes-by-parent facebook.com
    [{:modified "2023-04-08 20:11:04",
      :id 1,
      :parent "facebook.com",
      :key :pwd,
      :value "KGn'F8f9z1<<Q[%AA8$",
      :version 1,
      :created "2023-04-08 20:11:04"}
     {:key :user-id,
      :value "philippak@gmail.com",
      :version 1,
      :created "2023-04-08 20:19:23",
      :modified "2023-04-08 20:19:23",
      :id 2,
      :parent "facebook.com"}]

Hold, there must be a quicker way... there is!
Let's first delete our work.

    pass> :delete-parent facebook.com
    {:rows-affected 2, :last-inserted-id 0}

`:list` gives us a list of parent to key tuples in the db.

    pass> :list
    no result returned.

Yup, they're gone.

And now to reinstate the quick way...

    pass> :add-multi facebook.com :user-id "philippak@gmail.com" :pwd "KGn'F8f9z1<<Q[%AA8$"
    ({:id 1, :parent "facebook.com"} {:id 2, :parent "facebook.com"})

`:add-multi` creates one or more nodes at once under the same parent.

    pass> :help :add-multi
    :add-multi
    -------------------------
    arglists:  ([parent & kvs])

    Adds multiple nodes specified by kvs (a list of keys and their values) under
    the parent. e.g. `add-multi facebook :user jude :pwd 6%%fdgWo`.

BTW you can do `:help :cmd-name` for any command.

> ** A quick note on reading `arglists` (arguments lists) for a command's help**
> (it can be quite terse!)
> In the above example, `()` means a list (in Clojure)
> `[]` means a vector. Like a list, a sequence of items. Confused yet? Just think of it as another type of list!
> In the `[]` we see the arguments required for this function:
> parent is first and then `&` just means 'and a sequence of .. in this case `kvs`, key value pairs of course!

Let's look at another command's arglists.

    pass> :help :get-node
    :get-node
    -------------------------
    arglists:  ([key] [parent key])

    Returns the decrypted node specified by either just the key (in which case
    the default parent is used - if set, or `:root` if not) or by both the parent

> Here the :get-node function has two `[]`'s inside the arglists, so two different ways of calling it.
> The first is with just a key, and the second with both a parent and a key. Let's try the second way.

    pass> :get-node facebook.com user-id
    Unsuccessful

Oops, I forgot that I used a keyword `:user-id` rather than the string `"user-id"`. Try again.

    pass> :get-node facebook.com :user-id
    {:modified "2023-04-08 20:27:51",
     :id 1,
     :parent "facebook.com",
     :key :user-id,
     :value "philippak@gmail.com",
     :version 1,
     :created "2023-04-08 20:27:51"}

### <a name="structuring"></a>Structuring your data

*Let's move on from facebook.com* and make a journal entry instead.

    pass> :add-node :private sleep-journal "Today I slept blissfully well!"
    {:id 3, :parent :private}

> <a name="doublequotes"></a>**When to put strings in double quotes**
>
> A string containing any of the characters:
> *space/s*
> \` (*backtick*)
> `#`
> `~`
> `@`
> `[`
> `]`
> `(`
> `)`
> `{`
> `}`
> must be double quoted!
> e.g. "Iam@a]{(pass`W0rd~"
> e.g. "I am fine!"
> e.g. facebook.com    - Ok not to double quote, as no spaces or special characters.


It's tomorrow now and time to add my latest entry to my sleep-journal.
Hmmm, the way I structured it that's not too easy to add a new entry. To use date as the key could be logical. Let's delete yesterday's and begin again.

    pass> :delete-node :private sleep-journal
    {:rows-affected 1, :last-inserted-id 0}

I *could* do `:add-node sleep-journal "20th May" "Today I slept blissfully well!"` (and that would work.) but after a time, my whole database would be mainly sleep jounral entries and someone might think I was obsessed or something. There must be a more compact way of storing complex data.

Clojure data structures to the rescue!

#### maps

> `{key1 value1 key2 value2 ... }` is a map; a mapping of keys and their values. Just what we need here.

</br>

    pass> :add-node :private sleep-journal {"20th May" "Today I slept blissfully well!"}
    {:id 3, :parent :private}

Let's have a look at that node.

    pass> :get-node :private sleep-journal
    {:key "sleep-journal",
     :value {"20th May" "Today I slept blissfully well!"},
     :version 1,
     :created "2023-04-08 20:55:28",
     :modified "2023-04-08 20:55:28",
     :id 3,
     :parent :private}

Now you see that the whole result returned is a map `{}`, and the `:value` is also a map.

We need a way to update the sleep-journal map and add the next fascinating entry!

Pass makes a few Clojure functions for editing data structures available: `assoc`, `dissoc`, `update`, `update-in`, `str`, `conj` and `merge` that can be used with the pass function `:update-node-with`. These should cover most needs.

With our new compact sleep-journal, let's use `assoc` (associate) to add a new key and value into the map.

    pass> :update-node-with :private sleep-journal assoc "21st May" "Another good night"
    {:rows-affected 1, :last-inserted-id 0}

and check that it worked.

    pass> :get-node :private sleep-journal
    {:id 3,
     :parent :private,
     :key "sleep-journal",
     :value
     {"20th May" "Today I slept blissfully well!",
      "21st May" "Another good night"},
     :version 2,
     :created "2023-04-09 07:52:04",
     :modified "2023-04-09 07:52:11"}

 
Let's look at the other Clojure functions that work on maps `{ ... }`
> 
> `dissoc` (dissociate) is the opposite of `assoc` and removes a key entry from a map.
> e.g.  `:update-node-with :private sleep-journal dissoc "21st May"`
>
> `merge` merges two maps together.
> e.g. `:update-node-with :private sleep-journal merge {"21st May" "Another good night"}`
>
> and a bit more advanced are `update` and `update-in`...

Say you wanted to say a bit more in the 21st May entry. You meant to say: "Another good night until the dog started howling at 5am!"

    pass> :update-node-with :private sleep-journal update "21st May" str "until the dog started howling at 5am!"
    {:rows-affected 1, :last-inserted-id 0}

> `str` is one of the other functions that's available. It's used to append a string onto an existing.

    pass> :get-node :private sleep-journal
    {:created "2023-04-09 07:52:04",
     :modified "2023-04-09 17:18:57",
     :id 3,
     :parent :private,
     :key "sleep-journal",
     :value
     {"20th May" "Today I slept blissfully well!",
      "21st May"
      "Another good nightuntil the dog started howling at 5am!"},
     :version 3}

Whoops, we forgot to an a space in front of 'until ...'

We can **`:restore`** the previous version quite easily.

    pass> :restore :private sleep-journal
    {:rows-affected 1, :last-inserted-id 0}
    
    pass> :get-node :private sleep-journal
    {:version 4,
     :created "2023-04-09 07:52:04",
     :modified "2023-04-09 17:22:44",
     :id 3,
     :parent :private,
     :key "sleep-journal",
     :value
     {"20th May" "Today I slept blissfully well!",
      "21st May" "Another good night"}}

and then update again.. this time correctly!

Finally, for maps, `update-in` is for doing updates (deep) within nested maps. It takes a *vector* of keys to reach that nested entry rather than the single key that `update` takes.


#### Lists and Vectors

Let's imagine you wanted to make a secret shopping list.

    pass> :add-node :private shopping-list (champagne cake balloons)
    {:id 4, :parent :private}

You must be planning a surprise party!

`(...)` is a list. Lists are good for adding to the beginning of with the `conj` function.

For example,

    pass> :update-node-with :private shopping-list conj "paper napkins"
    {:rows-affected 1, :last-inserted-id 0}
    
    pass> :get-node :private shopping-list
    {:created "2023-04-10 06:28:03",
     :modified "2023-04-10 06:28:10",
     :id 4,
     :parent :private,
     :key "shopping-list",
     :value ("paper napkins" "champagne" "cake" "balloons"),
     :version 2}

`[...]` is a vector. Vectors are good for adding to at the end, again with the `conj` function.

    pass> :delete-node :private shopping-list
    {:rows-affected 1, :last-inserted-id 0}
    
    pass> :add-node :private shopping-list [champagne cake balloons]
    {:id 4, :parent :private}
    
    pass> :update-node-with :private shopping-list conj "paper napkins"
    {:rows-affected 1, :last-inserted-id 0}
    
    pass> :get-node :private shopping-list
    {:parent :private,
     :key "shopping-list",
     :value ["champagne" "cake" "balloons" "paper napkins"],
     :version 2,
     :created "2023-04-10 06:32:41",
     :modified "2023-04-10 06:33:01",
     :id 4}


### other commands

#### Defaulting the parent

If you're planning to build an encrypted database which doesn't need to be divided up into sections, for example a password manager, then you might choose to have the same `parent` for every node so that you don't need to think about it.

Let's go back to our `facebook.com` example and structure it a slightly different way with your new knowledge of how to store data structures in pass.

    pass> :set-default-parent :root
    :root

check it..

    pass> :get-default-parent 
    :root

If you set the default-parent, it's persisted in the database, so is still set the next time you log in.


All the functions where you specify a node by it's parent and key, for example `:update-node-with` that we've been working with, don't need you to specify a parent. We can see that by looking in the `arglists` in the `:help`.

    pass> :help :update-node-with
    :update-node-with
    -------------------------
    arglists: ([key f & args][parent key f & args])

    Updates the node specified by parent and k's value by applying the function
    f to the old value and arg(uments).
    Available functions to use for f are:
    `assoc`, `dissoc` for adding and removing keys from a map.
    `update`, `update-in` for updating values inside, deep inside a map.
    `str` for adding to an existing string.
    `merge` for merging two maps.
    `conj` for adding items to a list or vector respectively.
    Please see Clojure's help online for how to use each of these functions.


So, let's add our facebook details in a single node using a map.


    pass> :add-node facebook.com {:user-id "philippak@gmail.com" :pwd "KGn'F8f9z1<<Q[%AA8$"}
    {:id 5, :parent :root}

    pass> :get-node facebook.com
    {:created "2023-04-10 07:09:11",
     :modified "2023-04-10 07:09:11",
     :id 5,
     :parent :root,
     :key "facebook.com",
     :value {:user-id "philippak@gmail.com" :pwd "KGn'F8f9z1<<Q[%AA8$"},
     :version 1}

If you're comfortable with the `{...}`, `(...)` and `[...]` data structures and particularly with the `:update-node-with` function that we've looked at in some detail, then defaulting the parent can give you a slightly more terse way of working.

#### generating a password

The `:gen` function provides a way for you to generate a random password.


#### format

When you're querying multiple nodes from the database, it can be helpful to see the result formatted in different ways to the standard which is `:edn` (Clojure's Extensible Data Notation).

For example as a table:

    pass> :fmt :table
    ok
    
    pass> :get-all-nodes :decrypt? true
    +--------------+---------+---------------+---------------+----+--------------+---------------+
    | value        | version | created       | modified      | id | parent       | key           |
    +--------------+---------+---------------+---------------+----+--------------+---------------+
    | philippak... | 1       | 2023-04-08... | 2023-04-08... | 1  | facebook.com | :user-id      |
    | KGn'F8f9z... | 1       | 2023-04-08... | 2023-04-08... | 2  | facebook.com | :pwd          |
    | {"20th Ma... | 4       | 2023-04-09... | 2023-04-09... | 3  | :private     | sleep-journal |
    | ["champag... | 2       | 2023-04-10... | 2023-04-10... | 4  | :private     | shopping-list |
    | {:user-id... | 1       | 2023-04-10... | 2023-04-10... | 5  | :root        | facebook.com  |
    +--------------+---------+---------------+---------------+----+--------------+---------------+

or as json

    pass> :fmt :json
    ok

    pass> :get-all-nodes :decrypt? true
    [{"key":"user-id","value":"philippak@gmail.com","version":1,"created":"2023-04-08 20:27:51","modified":"2023-04-08 20:27:51","id":1,"parent":"facebook.com"},{"version":1,"created":"2023-04-08 20:27:52","modified":"2023-04-08 20:27:52","id":2,"parent":"facebook.com","key":"pwd","value":"KGn'F8f9z1<<Q[%AA8$"},{"version":4,"created":"2023-04-09 07:52:04","modified":"2023-04-09 17:22:44","id":3,"parent":"private","key":"sleep-journal","value":{"20th May":"Today I slept blissfully well!","21st May":"Another good night"}},{"id":4,"parent":"private","key":"shopping-list","value":["champagne","cake","balloons","paper napkins"],"version":2,"created":"2023-04-10 06:32:41","modified":"2023-04-10 06:33:01"},{"id":5,"parent":":root","key":"facebook.com","value":"{:user-id philippak@gmail.com :pwd KGn'F8f9z1<<Q[%AA8$}","version":1,"created":"2023-04-10 07:09:11","modified":"2023-04-10 07:09:11"}]


#### Import and export

The `:import` and `:export` functions are there to get data into and out of the database. `:import` reads a file (you can control the format) and `:export` produces a file.

Please see the `:help` on these functions for more details.

#### get-in-node and copy-in-node

These two functions are especially useful for digging data out of data structures stored in a node's value. For example, say you have a node like this..

    {:modified "2023-04-11 21:53:31",
     :id 4,
     :parent :root,
     :key "facebook.com",
     :value {:user-id "philippak@gmail.com", :pwd "KGn'F8f9z1<<Q[%AA8$"},
     :version 1,
     :created "2023-04-11 21:53:31"}

and you wanted to dig out just the password.

    pass> :get-in-node facebook.com [:pwd]
    KGn'F8f9z1<<Q[%AA8$

Allows you to get the password directly. `:copy-in-node` is called in the same way, but instead of display the retrieved data, copies it directly to the clipboard - useful for passwords for example.

## License

Copyright © 2023 Jude Payne

Distributed under the [MIT License](http://opensource.org/licenses/MIT)

