# All-Terrain Tripodal Attackers
ATTA or ATTA-Vehicle (ATTA-V) for short

All-Terrain* Tripodal Attackers is a mod inspired by 
[Armistice: The Peace Engines](https://modrinth.com/mod/armistice), 
[Synthetic Selection](https://www.youtube.com/watch?v=iNXzOuc9UWo), 
and various other procedural animation techniques/works.

It currently adds only one entity, the Wanderer (in-game, this is the only ATTA, so it is named as such).

The Wanderer will walk around and follow nearby players. It uses its multiple legs to traverse the surrounding terrain.
The legs are rendered as simple lines, with the currently moving leg being rendered in red and all others black.

Wanderers can be ridden by players. Simply use (right-click) the entity while within reach to override its "brain" and take control.

### Gamerules
- atta_v:wandererStompDoesDamage - this gamerule controls whether the Wanderer does damage to nearby entities when its leg hits the ground.
- atta_v:wandererSeeksOutPlayers - this gamerule controls whether the Wanderer tries to find player targets to follow

### Commands
- /attav recalibratelegs - can be used by the controlling passenger of a Wanderer when it gets stuck to reset the leg positions.
- /attav recalibratelegs <entityselector> - can be used by command sources with a permission level >= 2. Resets the leg positions of a Wanderer specified in the selector.

*Wanderers are terrible at walking up cliffs and are very good at getting stuck in random caves in the ground.