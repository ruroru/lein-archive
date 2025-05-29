[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.jj/archive.svg)](https://clojars.org/org.clojars.jj/archive)

# lein archive

Lein archive is a leiningen plugin that adds a given set of files to an archive


## Installation

```clojure
[org.clojars.jj/lein-archive "1.0.0"]
```

## Usage

Configure archive creation in your project.clj under the top-level :archive key.

for example:

```clojure
:archive {:format      :tgz
          :file-name "target/archive.tgz"
          :file-set  [{:source-path "target/dependencies/file-*.jar" :output-path "/jar-files/"}
                      {:source-path "config" :output-path "/"}
                      {:source-path "target/application.jar" :output-path "/jar-files"}]}
```

### Executing plugin
```shell
lein archive
```

### Supported types

| supported formats |
|-------------------|
| tgz               |
| zip               |

## License

Copyright © 2025 [ruroru](https://github.com/ruroru)

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
