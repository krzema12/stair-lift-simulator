# Stair lift simulator

On Ubuntu, it crashes after about 1 minute. Running this before `./gradew runJvm` mitigates the problem:

```
export MESA_LOADER_DRIVER_OVERRIDE=i965
```
