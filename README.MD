# Chatix
Server-side mod that provides separate chats for your server.

## How to use
Create `.yml` file in `config/chatix/chat_groups` and fill it\
Example global chat group:
```yaml
global: # Group name
  # Default value = ""
  prefix: "!" # Prefix for this group
  # Default value = -1
  distance: -1 # On what distance players can hear 
    # other players in this group.
    # If distance > 0 everyone in radius of distance will see your message
    # If distance = 0 everyone in world will see your message
    # If distance < 0 everyone on server will see your message
    # Default value = 0
  priority: 1 # Group priority
  # Default value = true
  defaultPermission: true # Can everyone access this group
    # by default
  # Default value = JSON
  deserializer: JSON # Deserializer used to create component
    # out of output
    # Right now there is only one deserializer: JSON
  # Required
  output: '{"extra":[{"color":"gold","text":"[G]"},{"text":" ${player_name}: ${message}"}],"text":""}'
    # This field don't have default value, and required.
```
Every group described in file will be loaded, so you can add as many groups as you want.

Groups with `defaultPermission: false` will require user to have `chatix.group.[GROUP_NAME].receive` to receive message from this group, and `chatix.group.[GROUP_NAME].send` to send messages to this group.

**THIS MOD IS VERY W.I.P AND HAVE A LOT OF BUGS**