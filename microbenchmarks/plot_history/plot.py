#
# Plots number of iterations vs number of unique states for MonkeyDB applications.
#
# Args:
#   Log directory containing sub-folders for each app (twitter, shopping_cart,
#   courseware, stack)
#   Number of iterations
#
import sys, os, time
import hashlib
import re
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime
from functools import reduce
import pandas as pd
import numpy as np
from plotly.subplots import make_subplots
import pickle
import traceback


folders = ["twitter", "shopping_cart", "courseware", "stack"]

# The numbers used for the experiments in the paper:
# num_iters = [30000, 30000, 56000, 56000]
# plot_iters = [5000, 5000, 300, 2000]
num_iters = [int(sys.argv[2])] * 4
plot_iters = num_iters

groups = ["group1", "group2", "group3", "group4", "group5", "group6"]
colors = ["","indigo","green", "", "orange","royalblue"]

# Average states explored plot
fig_avg = make_subplots(rows=2, cols=2,
                        x_title="Number of Iterations",
                        y_title="Avg. Unique States",
                        horizontal_spacing=0.1,
                        vertical_spacing=0.13,
                        subplot_titles=('Shopping Cart',  'Twitter',
                                        'Courseware', 'Stack'))

def parse_shopping_cart_logs(index):
    log_dir    = sys.argv[1] + folders[index]
    file_count = len(os.listdir(log_dir))
    files      = os.listdir(log_dir)
    num_iter   = num_iters[index]
    plot_iter  = plot_iters[index]

    exp_count = 0
    logs = []

    # skip dirs
    for file in range(file_count):
        log              = os.path.join(log_dir, files[file])
        if os.path.isdir(log):
            print("directory skipping " + log)
        else:
            exp_count += 1

    y         = [[] for i in range(exp_count)]
    exec_time = [0 for i in range(exp_count)]
    y_random  = [[] for i in range(exp_count)]

    logs = ["causal_max", "causal_delay", "causal", "serializability_max",
            "serializability_delay", "serializability"]


    for exp in range(exp_count):
        log              = os.path.join(log_dir, logs[exp])
        iter_unique      = [0 for x in range(num_iter + 1)]
        hash_seen_so_far = {}
        i                = 0
        random_states    = []
        random_runs      = 0

        if os.path.isdir(log):
            print("directory skipping " + log)
            # skip directories
            continue


        # print(log)
        try:
            with open(log, 'r') as f:
                get_return_values = {}

                for line in f:
                    words   = list(filter(None, re.split('\s+', line)))
                    if words[4] == 'Iteration' and words[6] == 'end':
                        union = []
                        for key, value in sorted(get_return_values.items()):
                            union.append(hash(tuple(value)))
                        if len(union) == 0:
                            iter_unique[i + 1] = iter_unique[i]
                        else:
                            hash_val = reduce(lambda x, y: x * y, union)

                            if hash_val in hash_seen_so_far:
                                hash_seen_so_far[hash_val] += 1
                                iter_unique[i + 1] = iter_unique[i]
                            else:
                                hash_seen_so_far[hash_val] = 1
                                iter_unique[i + 1] = iter_unique[i] + 1

                        exec_time[exp] += int(words[7])
                        i += 1
                        get_return_values = {}
                        continue

                    if words[4] == 'Iteration':
                        continue

                    if words[4] == 'RANDOM':
                        if words[6] == 'end':
                            # end of one test case
                            # fill rest of iterations
                            for k in range(i - 1, num_iter):
                                iter_unique[k + 1] = iter_unique[k]

                            random_states.append(iter_unique[-1])
                            # print(str(random_runs) + " , " + str(iter_unique[-1]))
                            if (iter_unique[-1] != 0):
                                if exp == 0 or exp == 3:
                                    y_random[exp].append([iter_unique[-1] for _ in range(num_iter)])
                                else:
                                    y_random[exp].append(iter_unique)
                            # reset state
                            iter_unique      = [0 for x in range(num_iter + 1)]
                            hash_seen_so_far = {}
                            i                = 0
                            random_runs     += 1

                        continue


                    session = words[5]
                    value = {}
                    for k in range(6, len(words) - 1, 2):
                        value[words[k]] = int(words[k + 1])

                    if len(value) != 0:
                        if session not in get_return_values:
                            get_return_values[session] = [tuple(value.items())]
                        else:
                            get_return_values[session].append(tuple(value.items()))


        except Exception as e:
            print("Exception at iteration " + str(i))
            # iter_unique[i + 1] = iter_unique[i]
            print(e)
        # print("Random runs " + str(random_runs))

        y[exp] = random_states

    # print("num of iterations: " + str(num_iter))

    # Dump processed logs, can be used in subsequent runs to avoid parsing logs
    with open('shopping_cart.log', 'wb') as fp:
        pickle.dump(y_random, fp)

    x = list(range(plot_iter + 1))

    # with open ('shopping_cart.log', 'rb') as fp:
    #     y_random = pickle.load(fp)

    # filter_threshold = np.array(y_random[0])[:,-1] > 5

    for i in range(exp_count):
        # restrict num of iterations
        y_avg = np.array(y_random[i])[0:,:plot_iter + 1]
        # filter based on threshold states
        # y_avg = y_avg[filter_threshold]
        # print(logs[i] + str(y_avg.shape))
        if (i == 0):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         line=dict(color='firebrick', dash='dot', width=6)),
                              row=1,col=1)
        elif (i == 3):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         line=dict(color='rgb(34,34,34)', dash='dot', width=6)),
                              row=1,col=1)
        else:
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         line=dict(color=colors[i],width=5)),
                              row=1,col=1)



def parse_twitter_logs(index):
    log_dir    = sys.argv[1] + folders[index]
    file_count = len(os.listdir(log_dir))
    files      = os.listdir(log_dir)
    num_iter   = num_iters[index]
    plot_iter  = plot_iters[index]


    exp_count = 0
    logs = []

    # skip dirs
    for file in range(file_count):
        log              = os.path.join(log_dir, files[file])
        if os.path.isdir(log):
            print("directory skipping " + log)
        else:
            exp_count += 1

    y         = [[] for i in range(exp_count)]
    exec_time = [0 for i in range(exp_count)]
    y_random  = [[] for i in range(exp_count)]
    logs = ["causal_max", "causal_delay", "causal", "serializability_max",
            "serializability_delay", "serializability"]


    for exp in range(exp_count):

        log              = os.path.join(log_dir, logs[exp])
        iter_unique      = [0 for x in range(num_iter + 1)]
        hash_seen_so_far = {}
        i                = 0
        random_states    = []
        random_runs      = 0


        # print(log)
        try:
            with open(log, 'r') as f:
                get_return_values = []

                for line in f:
                    words   = list(filter(None, re.split('\s+', line)))
                    if (len(words) < 5):
                        continue
                    if words[4] == 'Iteration' and words[6] == 'end':
                        union = []
                        for value in get_return_values:
                            union.append(hash(value))
                        # hash_val = hash(tuple(union))
                        if len(union) == 0:
                            iter_unique[i + 1] = iter_unique[i]
                        else:
                            hash_val = reduce(lambda x, y: x * y, union)
                            # hash with multiplicaiton

                            if hash_val in hash_seen_so_far:
                                hash_seen_so_far[hash_val] += 1
                                iter_unique[i + 1] = iter_unique[i]
                            else:
                                hash_seen_so_far[hash_val] = 1
                                iter_unique[i + 1] = iter_unique[i] + 1

                        exec_time[exp] += int(words[7])
                        i += 1
                        get_return_values = []
                        continue

                    if words[4] == 'Iteration':
                        continue

                    if words[4] == 'RANDOM':
                        if words[6] == 'end':
                            # end of one random run
                            for k in range(i - 1, num_iter):
                                iter_unique[k + 1] = iter_unique[k]

                            random_states.append(iter_unique[-1])
                            if (iter_unique[-1] != 0):
                                if exp == 0 or exp == 3:
                                    y_random[exp].append([iter_unique[-1] for _ in range(num_iter)])
                                else:
                                    y_random[exp].append(iter_unique)

                            # reset state
                            iter_unique      = [0 for x in range(num_iter + 1)]
                            hash_seen_so_far = {}
                            i                = 0
                            random_runs     += 1

                        continue


                    s = re.findall('\((.*?)\)', line)
                    value = {}
                    for element in s:
                        e = re.split(': | \s+', element)
                        neighbors = []
                        for k in range(1, len(e)):
                            if e[k] != '':
                                neighbors.append(e[k])
                        value[e[0]] = tuple(neighbors)


                    if len(value) != 0:
                        get_return_values.append(tuple(value.items()))


        except Exception as e:
            print("Exception at iteration " + str(i))
            # iter_unique[i + 1] = iter_unique[i]
            print(e)
            print(traceback.format_exc())
        # print("Random runs " + str(random_runs))

        y[exp] = random_states
        # print("num of iterations: " + str(num_iter))
        # print("num of unique states: " + str(len(hash_seen_so_far)))

    # Dump processed logs, can be used in subsequent runs to avoid parsing logs
    with open('twitter.log', 'wb') as fp:
        pickle.dump(y_random, fp)

    x = list(range(plot_iter + 1))

    # with open ('twitter.log', 'rb') as fp:
    #     y_random = pickle.load(fp)

    # filter_threshold = np.array(y_random[0])[:,-1] > 5

    for i in range(exp_count):
        # restrict num of iterations
        y_avg = np.array(y_random[i])[0:,:plot_iter + 1]

        # filter based on threshold states
        # y_avg = y_avg[filter_threshold]
        # print(logs[i] + str(y_avg.shape))
        if (i == 0):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color='firebrick', dash='dot', width=6)),
                              row=1,col=2)
        elif (i == 3):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color='rgb(34,34,34)', dash='dot', width=6)),
                              row=1,col=2)
        else:
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color=colors[i],width=5)),
                              row=1,col=2)


def parse_courseware_logs(index):
    log_dir    = sys.argv[1] + folders[index]
    file_count = len(os.listdir(log_dir))
    files      = os.listdir(log_dir)
    num_iter   = num_iters[index]
    plot_iter  = plot_iters[index]

    exp_count = 0
    logs = []

    # skip dirs
    for file in range(file_count):
        log              = os.path.join(log_dir, files[file])
        if os.path.isdir(log):
            print("directory skipping " + log)
        else:
            exp_count += 1

    y         = [[] for i in range(exp_count)]
    exec_time = [0 for i in range(exp_count)]
    y_random  = [[] for i in range(exp_count)]

    logs = ["causal_max", "causal_delay", "causal", "serializability_max",
            "serializability_delay", "serializability"]

    for exp in range(exp_count):

        log              = os.path.join(log_dir, logs[exp])
        iter_unique      = [0 for x in range(num_iter + 1)]
        hash_seen_so_far = {}
        i                = 0
        random_states    = []
        random_runs      = 0


        # print(log)
        try:
            with open(log, 'r') as f:
                get_return_values = []

                for line in f:
                    words   = list(filter(None, re.split('\s+', line)))
                    if (len(words) < 5):
                        continue
                    if words[4] == 'Iteration' and words[6] == 'end':
                        union = []
                        for value in get_return_values:
                            union.append(hash(value))
                        # hash_val = hash(tuple(union))
                        if len(union) == 0:
                            iter_unique[i + 1] = iter_unique[i]
                        else:
                            hash_val = reduce(lambda x, y: x * y, union)
                            # hash with multiplicaiton

                            if hash_val in hash_seen_so_far:
                                hash_seen_so_far[hash_val] += 1
                                iter_unique[i + 1] = iter_unique[i]
                            else:
                                hash_seen_so_far[hash_val] = 1
                                iter_unique[i + 1] = iter_unique[i] + 1

                        exec_time[exp] += int(words[7])
                        i += 1
                        get_return_values = []
                        continue

                    if words[4] == 'Iteration':
                        continue

                    if words[4] == 'RANDOM':
                        if words[6] == 'end':
                            # end of one random run
                            for k in range(i - 1, num_iter):
                                iter_unique[k + 1] = iter_unique[k]

                            random_states.append(iter_unique[-1])
                            if (iter_unique[-1] != 0):
                                if exp == 0 or exp == 3:
                                    y_random[exp].append([iter_unique[-1] for _ in range(num_iter)])
                                else:
                                    y_random[exp].append(iter_unique)

                            # reset state
                            iter_unique      = [0 for x in range(num_iter + 1)]
                            hash_seen_so_far = {}
                            i                = 0
                            random_runs     += 1

                        continue


                    s = re.findall('\((.*?)\)', line)
                    value = {}
                    for element in s:
                        e = re.split(': | \s+', element)
                        neighbors = []
                        for k in range(1, len(e)):
                            if e[k] != '':
                                neighbors.append(e[k])
                        value[e[0]] = tuple(neighbors)


                    if len(value) != 0:
                        get_return_values.append(tuple(value.items()))


        except Exception as e:
            print("Exception at iteration " + str(i))
            # iter_unique[i + 1] = iter_unique[i]
            print(e)
        # print("Random runs " + str(random_runs))

        y[exp] = random_states
        # print("num of iterations: " + str(num_iter))
        # print("num of unique states: " + str(len(hash_seen_so_far)))

    # Dump processed logs, can be used in subsequent runs to avoid parsing logs
    with open('courseware.log', 'wb') as fp:
        pickle.dump(y_random, fp)

    x = list(range(plot_iter + 1))

    # with open ('courseware.log', 'rb') as fp:
    #     y_random = pickle.load(fp)

   
    # filter_threshold = np.array(y_random[0])[:,-1] > 5

    for i in range(exp_count):
        # restrict num of iterations
        y_avg = np.array(y_random[i])[0:,:plot_iter + 1]
        # filter based on threshold states
        # y_avg = y_avg[filter_threshold]
        # print(logs[i] + str(y_avg.shape))
        if (i == 0):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color='firebrick', dash='dot', width=6)),
                              row=2,col=1)
        elif (i == 3):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color='rgb(34,34,34)', dash='dot', width=6)),
                              row=2,col=1)
        else:
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color=colors[i],width=5)),
                              row=2,col=1)


def parse_stack_logs(index):
    log_dir    = sys.argv[1] + folders[index]
    file_count = len(os.listdir(log_dir))
    files      = os.listdir(log_dir)
    num_iter   = num_iters[index]
    plot_iter  = plot_iters[index]


    exp_count = 0
    logs = []

    # skip dirs
    for file in range(file_count):
        log              = os.path.join(log_dir, files[file])
        if os.path.isdir(log):
            print("directory skipping " + log)
        else:
            exp_count += 1

    y         = [[] for i in range(exp_count)]
    exec_time = [0 for i in range(exp_count)]
    y_random  = [[] for i in range(exp_count)]

    logs = ["causal_max", "causal_delay", "causal", "serializability_max",
            "serializability_delay", "serializability"]

    for exp in range(exp_count):

        log              = os.path.join(log_dir, logs[exp])
        iter_unique      = [0 for x in range(num_iter + 1)]
        hash_seen_so_far = {}
        i                = 0
        random_states    = []
        random_runs      = 0


        # print(log)

        try:
            with open(log, 'r') as f:
                get_return_values = {}
                max_states = 0

                for line in f:
                    words   = list(filter(None, re.split('\s+', line)))
                    if words[4] == 'Iteration' and words[6] == 'end':
                        # print(i, return_values)
                        union = []
                        for key, value in sorted(get_return_values.items()):
                            union.append(hash(tuple(value)))
                        # print(str(union) + " " +  words[5] + " " + log)
                        # hash_val = hash(tuple(union))
                        if len(union) == 0:
                            iter_unique[i + 1] = iter_unique[i]
                        else:
                            hash_val = reduce(lambda x, y: x * y, union)
                            # hash with multiplicaiton

                            if hash_val in hash_seen_so_far:
                                hash_seen_so_far[hash_val] += 1
                                iter_unique[i + 1] = iter_unique[i]
                            else:
                                hash_seen_so_far[hash_val] = 1
                                iter_unique[i + 1] = iter_unique[i] + 1

                        exec_time[exp] += int(words[7])
                        i += 1
                        get_return_values = {}
                        continue

                    if words[4] == 'Iteration':
                        continue

                    if words[4] == 'RANDOM':
                        if words[6] == 'end':
                            # end of one random run
                            for k in range(i - 1, num_iter):
                                iter_unique[k + 1] = iter_unique[k]

                            random_states.append(iter_unique[-1])
                            if (iter_unique[-1] != 0):
                                if exp == 0 or exp == 3:
                                    y_random[exp].append([iter_unique[-1] for _ in range(num_iter)])
                                else:
                                    y_random[exp].append(iter_unique)

                            max_states = max(max_states, iter_unique[-1])

                            # reset state
                            iter_unique      = [0 for x in range(num_iter + 1)]
                            hash_seen_so_far = {}
                            i                = 0
                            random_runs     += 1

                        continue


                    value   = words[5]
                    session = words[6]

                    if session not in get_return_values:
                        get_return_values[session] = [value]
                    else:
                        get_return_values[session].append(value)


        except Exception as e:
            # iter_unique[i + 1] = iter_unique[i]
            print(e)
        # print ("Random runs " + str(random_runs) + " max states " + str(max_states))

        y[exp] = random_states

        # print("num of iterations: " + str(num_iter))

    # Dump processed logs, can be used in subsequent runs to avoid parsing logs
    with open('stack.log', 'wb') as fp:
        pickle.dump(y_random, fp)

    x = list(range(plot_iter + 1))

    # with open ('stack.log', 'rb') as fp:
    #     y_random = pickle.load(fp)


    # filter_threshold = np.array(y_random[0])[:,-1] > 5

    for i in range(exp_count):
        # restrict num of iterations
        y_avg = np.array(y_random[i])[0:,:plot_iter + 1]
        # filter based on threshold states
        # y_avg = y_avg[filter_threshold]
        # print(logs[i] + str(y_avg.shape))
        if (i == 0):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color='firebrick', dash='dot', width=6)),
                              row=2,col=2)
        elif (i == 3):
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color='rgb(34,34,34)', dash='dot', width=6)),
                              row=2,col=2)
        else:
            fig_avg.add_trace(go.Scatter(x=x, y=y_avg.mean(0),
                                         mode='lines',
                                         name=logs[i],
                                         legendgroup=groups[i],
                                         showlegend=False,
                                         line=dict(color=colors[i],width=5)),
                              row=2,col=2)





for index in range(len(folders)):
    if folders[index] == "shopping_cart":
        parse_shopping_cart_logs(index)
    elif folders[index] == "twitter":
        parse_twitter_logs(index)
    elif folders[index] == "courseware":
        parse_courseware_logs(index)
    elif folders[index] == "stack":
        parse_stack_logs(index)


fig_avg.layout.annotations[5]["xshift"] = -80
fig_avg.layout.annotations[4]["yshift"] = -70
for i in fig_avg['layout']['annotations']:
    i['font'] = dict(size=40,color='#000000',family="Dejavu Sans")

fig_avg.update_layout(
    margin=dict(t=90,l=130,b=120),
    legend=dict(
        font=dict(
            family="Dejavu Sans",
            color="black",
            size=42
        ),
        y=0.5,
        x=1.05,
        # orientation="h",
        borderwidth=3
    ),
    font=dict(
            family="Dejavu Sans",
            color="black",
            size=34
    ),
    plot_bgcolor='rgb(255,255,255)'
)


fig_avg.update_xaxes(rangemode="nonnegative", showgrid=False, showline=True, linewidth=1, linecolor='black')

fig_avg.update_yaxes(rangemode="nonnegative", showgrid=False, showline=True, linewidth=1, linecolor='black')

fig_avg.write_image("plot.pdf", width=2500, height=1500)

