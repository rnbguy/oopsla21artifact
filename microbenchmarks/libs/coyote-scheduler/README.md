A C++ library for **language-agnostic systematic concurrency testing** by the
[Coyote](https://microsoft.github.io/coyote/) team in [Microsoft
Research](https://www.microsoft.com/en-us/research/).

The library provides APIs that allow you to instrument your runtime for (1) declaring concurrent
_operations_ and synchronization _resources_. (2) taking control of these operations and resources,
and (3) systematically exploring _interleavings_.

The library exposes an FFI for interop with other languages.

To learn more about the research behind our technology, check out our published papers
[here](https://microsoft.github.io/coyote/learn/resources/publications).

## How to build
On Linux, run the following from the root directory:
```bash
mkdir build
cd build
cmake -G "Ninja" -DCMAKE_BUILD_TYPE=Release  ..
ninja
```

To build for debug under Linux, add to the `-DCMAKE_BUILD_TYPE=Debug` flag when invoking `cmake`.

On Windows, open the project as a Visual Studio 2019 CMake project by selecting the CMakeLists.txt
file and then build the project.

After building the project, you can find a static and shared library in `bin`.

## How to use
To use the Coyote scheduler in a C++ project, link the static or shared library to your project, and
include the following header file (from the [`include`](./include) directory):
```c++
#include "coyote/scheduler.h"
```

Then use the Coyote scheduling APIs to instrument your code similar to our examples
[here](./test/integration).

To use the FFI from a language that requires importing a `dll` or `so`, follow the build
instructions below to build the shared library.

## Contributing
This project welcomes contributions and suggestions. Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide a
CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repositories using our CLA.

## Code of Conduct
This project has adopted the [Microsoft Open Source Code of
Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of
Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact
[opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
