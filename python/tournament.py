from playGame import play_game
from create_new_bots import *

MAPS = [
    'ALandDivided',
    'CentralLake',
    "CentralSoup",
    "ChristmasInJuly",
    "ClearlyTwelveHorsesInASalad",
    "CosmicBackgroundRadiation",
    "CowFarm",
    "DidAMonkeyMakeThis",
    "FourLakeLand",
    "GSF",
    "Hills",
    "InADitch",
    "Infinity",
    "IsThisProcedural",
    "Islands",
    "OmgThisIsProcedural",
    "ProceduralConfirmed",
    "RandomSoup1",
    "RandomSoup2",
    "Soup",
    "SoupOnTheSide",
    "TwoForOneAndTwoForAll",
    "Volcano",
    "WaterBot",
    "WateredDown",
]

if __name__ == '__main__':
    
    bot_1 = (0, 0)
    # bot_2 = (0, 1)

    # bot_1_name = f'generation_{bot_1[0]}_id_{bot_1[1]}'
    # bot_2_name = f'generation_{bot_2[0]}_id_{bot_2[1]}'
    bot_1_name = f'generation_{bot_1[0]}_id_{bot_1[1]}'
    bot_2_name = 'grekiki25'
    
    # create_new_bot('grekiki25', bot_1[0], bot_1[1])
    # create_new_bot('grekiki25', bot_2[0], bot_2[1])

    first_player_score = 0
    second_player_score = 0

    for play_map in MAPS:
        game = play_game(bot_1_name, bot_2_name, play_map)
        if game == 'PLAYER 1 WINS':
            first_player_score += 1
        elif game == 'PLAYER 2 WINS':
            second_player_score += 1
        else:
            print('ERROR!!')
    
        print(play_map, first_player_score, second_player_score)

