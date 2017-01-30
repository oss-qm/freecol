= Modifiers =

Many aspects of the game (that can be expressed as numbers) can be
customized by modifiers - in the ruleset specification, as well as
in the loadable mods.

Modifiers can be seen as a simple mathematical function that's applied
to certain values and carry a parameter (the "value" attribute). The
ModifierType decide which functions gets applied, eg. ADDITIVE means
adding the Modifier's value to the original value.

Note that modifiers are yet largely undocumented and sometimes change
between FC releases, so some adaption logic is needed for supporting
older savegames.

== Modifier List ==

=== AMPHIBIOUS_ATTACK ===

=== ARTILLERY_AGAINST_RAID ===

=== ARTILLERY_IN_THE_OPEN ===

=== ATTACK_BONUS ===

=== BIG_MOVEMENT_PENALTY ===

=== BOMBARD_BONUS ===

=== BREEDING_DIVISOR ===

=== BREEDING_FACTOR ===

=== BUILDING_PRICE_BONUS ===

=== CARGO_PENALTY ===

=== COLONY_GOODS_PARTY ===

=== CONSUME_ONLY_SURPLUS_PRODUCTION ===

=== CONVERSION_ALARM_RATE ===

=== CONVERSION_SKILL ===

* defined by: unit types
* queried by: indian settlement
* changes the missionary's skill for converting natives to colonists

=== DEFENCE ===

=== EXPLORE_LOST_CITY_RUMOUR ===

=== EXPOSED_TILES_RADIUS ===

=== FORTIFIED ===

=== IMMIGRATION ===

=== LAND_PAYMENT_MODIFIER ===

=== LIBERTY ===

=== LINE_OF_SIGHT_BONUS ===

=== MINIMUM_COLONY_SIZE ===

=== MISSIONARY_TRADE_BONUS ===

=== MOVEMENT_BONUS ===

=== NATIVE_ALARM_MODIFIER ===

=== NATIVE_CONVERT_BONUS ===

=== OFFENCE ===

=== OFFENCE_AGAINST ===

=== PEACE_TREATY ===

=== POPULAR_SUPPORT ===

=== RELIGIOUS_UNREST_BONUS ===

=== SAIL_HIGH_SEAS ===

=== SHIP_TRADE_PENALTY ===

=== SMALL_MOVEMENT_PENALTY ===

=== SOL ===

=== TILE_TYPE_CHANGE_PRODUCTION ===

=== TRADE_BONUS ===

=== TRADE_VOLUME_PENALTY ===

* defined by: unit types
* queried by: indian settlement
* influences the amount of goods an indian settlement is willing to sell (if unit given)

=== TREASURE_TRANSPORT_FEE ===

=== WAREHOUSE_STORAGE ===

* defined by: building types
* queried by: colonies / buildings
* defines the amount of storage capacity a building donates to a colony
