from playGame import play_game
from create_new_bots import *
import threading
import json

from collections import deque

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

class Referee(threading.Thread):

    def __init__(self, ids, q, results):
        threading.Thread.__init__(self)
        self.id = ids
        self.q = q
        self.results = results

    def run(self):
        
        while True:
            
            if len(self.q) == 0:
                break
            
            bot1, bot2, play_map = self.q.popleft()
            game_result = play_game(bot1, bot2, play_map)
            if game_result == 'PLAYER 1 WINS':
                self.results[bot1][bot2][0] += 1
                self.results[bot2][bot1][1] += 1
                print(f'BOT {bot1} WINS VS BOT {bot2} ON MAP {play_map}')
            elif game_result == 'PLAYER 2 WINS':
                self.results[bot1][bot2][1] += 1
                self.results[bot2][bot1][0] += 1
                print(f'BOT {bot2} WINS VS BOT {bot1} ON MAP {play_map}')
            else:
                print('There was an error in current game')
                print(bot1, bot2, play_map)


def play_tournament(generation, bot_names, referees=4):

    results = {}
    for bot1 in bot_names:
        results[bot1] = {}
        for bot2 in bot_names:
            results[bot1][bot2] = [0, 0] # Wins : loses
    
    matches = deque()
    for bot1 in bot_names:
        for bot2 in bot_names:
            if bot1 != bot2:
                for play_map in MAPS:
                    matches.append((bot1, bot2, play_map))
    
    referees_force = []
    for i in range(referees):
        referee = Referee(i, matches, results)
        referee.start()
        referees_force.append(referee)
    
    for referee in referees_force:
        referee.join()

    print('TOURNAMENT IS OVER')
    
    with open(f'tournament_results/generation_{generation}.json', 'w') as outfile:
        json.dump(results, outfile, indent=2)
    
    with open(f'tournament_results/match_table_{generation}.txt', 'w') as outfile:
        standings = []

        for name in bot_names:

            total_wins = 0
            results_vs_opponents = []
            
            for opponent in bot_names:
                total_wins += results[name][opponent][0]
                results_vs_opponents.append(results[name][opponent][0])
            
            standings.append([total_wins, name] + results_vs_opponents)
        
        standings.sort(reverse=True)

        for result in standings:
            output_string = '{:<5}'.format(result[0])
            output_string += '{:<20} : '.format(result[1])
            for vs_opp in result[2:]:
                output_string += '{:<3} '.format(vs_opp)
            
            print(output_string, file=outfile)
        
        print('\n', file=outfile)
        for line in bot_names:
            print(line, file=outfile)
            
    for result in standings:
        print(result)




if __name__ == '__main__':

    generation = 3
    bots = []
    # for i in range(3):
    #     bot_name = f'generation_{generation}_id_{i}'
    #     create_new_bot('grekiki25', generation, i)
    #     bots.append(bot_name)
    
    bots.append('generation_0_id_0') # Previous winner
    bots.append('grekiki25')
    
    play_tournament(generation, bots)
    

    # bot_1 = (0, 0)
    # # bot_2 = (0, 1)

    # # bot_1_name = f'generation_{bot_1[0]}_id_{bot_1[1]}'
    # # bot_2_name = f'generation_{bot_2[0]}_id_{bot_2[1]}'
    # bot_1_name = f'generation_{bot_1[0]}_id_{bot_1[1]}'
    # bot_2_name = 'grekiki25'
    
    # # create_new_bot('grekiki25', bot_1[0], bot_1[1])
    # # create_new_bot('grekiki25', bot_2[0], bot_2[1])

    # first_player_score = 0
    # second_player_score = 0

    # for play_map in MAPS:
    #     game = play_game(bot_1_name, bot_2_name, play_map)
    #     if game == 'PLAYER 1 WINS':
    #         first_player_score += 1
    #     elif game == 'PLAYER 2 WINS':
    #         second_player_score += 1
    #     else:
    #         print('ERROR!!')
    
    #     print(play_map, first_player_score, second_player_score)

