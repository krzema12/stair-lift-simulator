# Stair lift simulator

## [Run online](https://krzema12.github.io/stair-lift-simulator/)

## Troubleshooting

On Ubuntu, it crashes after about 1 minute. Running this before `./gradew runJvm` mitigates the problem:

```
export MESA_LOADER_DRIVER_OVERRIDE=i965
```
