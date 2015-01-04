(ns neko.debug
  "Contains useful tools to be used while developing the application."
  (:require [neko log notify])
  (:import android.app.Activity
           java.util.WeakHashMap))

;;; Simplify REPL access to Activity objects.

(def ^WeakHashMap all-activities
  "Weak hashmap that contains mapping of namespaces or
  keywords to Activity objects."
  (WeakHashMap.))

(defmacro ^Activity *a
  "If called without arguments, returns the activity for the current
  namespace. A version with one argument will return the activity for
  the given object (be it a namespace or any other object)."
  ([]
   `(get all-activities '~(.name *ns*)))
  ([key]
   `(get all-activities ~key)))

;; This atom stores the last exception happened on the UI thread.
;;
(def ^:private ui-exception (atom nil))

(defn handle-exception-from-ui-thread
  "Displays an exception message using a Toast and stores the
  exception for the future reference."
  [e]
  (reset! ui-exception e)
  (neko.log/e "Exception raised on UI thread." :exception e)
  (when-let [ctx (:neko.context/context all-activities)]
    (neko.notify/toast ctx (str e) :long)))

(defn ui-e
  "Returns an uncaught exception happened on UI thread."
  [] @ui-exception)

(defmacro catch-all-exceptions [func]
  (if (:neko.init/release-build *compiler-options*)
    `(~func)
    `(try (~func)
          (catch Throwable e#
            (handle-exception-from-ui-thread e#)))))

(defn safe-for-ui*
  "Wraps the given function inside a try..catch block and notify user
  using a Toast if an exception happens."
  [f]
  (catch-all-exceptions f))

(defmacro safe-for-ui
  "A conditional macro that will protect the application from crashing
  if the code provided in `body` crashes on UI thread in the debug
  build. If the build is a release one returns `body` as is."
  [& body]
  `(safe-for-ui* (fn [] ~@body)))
