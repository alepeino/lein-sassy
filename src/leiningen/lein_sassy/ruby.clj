(ns leiningen.lein-sassy.ruby
  (:require [cemerick.pomegranate :as pom])
  (:import [org.jruby.embed ScriptingContainer LocalContextScope LocalVariableBehavior]
           [org.jruby RubyHash RubySymbol RubyArray RubyString]))

(defn make-container
  "Creates a Ruby scripting container, currently as a SINGLETON This ensures
  that context is preserved across threads, but may lead to issues. THREADSAFE
  means that the context must be recreated for every thread which is not
  what we want."
  []
  (ScriptingContainer. LocalContextScope/SINGLETON LocalVariableBehavior/PERSISTENT))

(defn make-runtime [container]
  (-> (.getProvider container) .getRuntime))

(defn make-rb-string [runtime string]
  (RubyString/newString runtime string))

(defn make-rb-symbol [runtime string]
  (RubySymbol/newSymbol runtime (name string)))

(defn make-rb-array [runtime coll]
  (let [array (RubyArray/newArray runtime)]
    (doseq [v coll] (.add array v))
    array))

(defn make-rb-hash [runtime clj-hash]
  (let [rb-hash (RubyHash. runtime)]
    (doseq [[k v] clj-hash]
      (let [key (make-rb-symbol runtime k)
            value (cond
                    (map? v) (make-rb-hash runtime v)
                    (coll? v) (make-rb-array runtime v)
                    (string? v) (make-rb-string runtime v)
                    (keyword? v) (make-rb-symbol runtime v)
                    :else v)]
        (.put rb-hash key value)))
    rb-hash))

(defn run-ruby [container scriptlet]
  (.runScriptlet container scriptlet))

(defn call-ruby-method
  ([container object methodName returnType]
   (.callMethod container object methodName returnType))
  ([container object methodName args returnType]
   (.callMethod container object methodName args returnType)))

(defn add-gemjars []
  (pom/add-classpath "resources/ruby-gems.jar"))

(defn require-gem [container gem-name]
  (run-ruby container (str "require '" (name gem-name) "';")))
