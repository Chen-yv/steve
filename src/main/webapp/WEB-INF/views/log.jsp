<%--

    SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
    Copyright (C) 2013-2026 SteVe Community Team
    All Rights Reserved.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

--%>
<%@ include file="00-header.jsp" %>
<style>
  #logPage { font-size: 13px; }
  #logPage .toolbar {
    display: flex; flex-wrap: wrap; gap: 8px; align-items: center;
    margin-bottom: 8px; padding: 6px 10px;
    background: #fff; border: 1px solid #e5e7eb; border-radius: 6px;
  }
  #logPage .toolbar .group { display: flex; flex-wrap: wrap; align-items: center; gap: 5px; min-width: 0; }
  #logPage .toolbar .spacer { flex: 1; }

  /* status indicator */
  #logPage .status-wrap { display: flex; align-items: center; gap: 6px; }
  #logPage .status-dot {
    display: inline-block; width: 9px; height: 9px; border-radius: 50%;
    transition: background 0.3s;
  }
  #logPage .status-dot.live { background: #22c55e; box-shadow: 0 0 4px rgba(34,197,94,0.5); }
  #logPage .status-dot.paused { background: #94a3b8; }
  #logPage .status-dot.reconnecting { background: #ef4444; box-shadow: 0 0 4px rgba(239,68,68,0.5); }
  #logPage .status-text { font-size: 12px; font-weight: 500; color: #475569; min-width: 3.5em; }

  /* buttons + inputs: one shared height and look */
  #logPage .btn {
    height: 26px; padding: 0 7px; font-size: 12px; border: 1px solid #d1d5db; border-radius: 4px;
    background: #fff; cursor: pointer; color: #374151; display: flex; align-items: center; gap: 4px;
    transition: background 0.15s, border-color 0.15s; box-sizing: border-box;
  }
  #logPage .btn:hover { background: #f3f4f6; border-color: #9ca3af; }
  #logPage .btn.active { background: #eff6ff; border-color: #3b82f6; color: #1d4ed8; }
  #logPage .btn svg { width: 11px; height: 11px; fill: currentColor; }

  #logPage .toolbar input[type=text] {
    height: 26px; padding: 0 8px; font-size: 12px; border: 1px solid #d1d5db; border-radius: 4px;
    flex: 0 1 150px; width: auto; min-width: 100px; max-width: 200px;
    outline: none; transition: border-color 0.15s; box-sizing: border-box;
  }
  #logPage .toolbar input[type=text]:focus { border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59,130,246,0.1); }
  #logPage .toolbar label {
    font-size: 12px; color: #6b7280; cursor: pointer; display: flex; align-items: center; gap: 4px;
    user-select: none; white-space: nowrap; height: 26px;
  }
  #logPage .toolbar label input[type=checkbox] { margin: 0; }

  /* level multi-select rows inside the dropdown */
  #logPage .dd-item .box {
    display: inline-block; width: 13px; height: 13px; margin-right: 6px;
    border: 1px solid #cbd5e1; border-radius: 3px; vertical-align: -2px;
    position: relative; box-sizing: border-box;
  }
  #logPage .dd-item.sel .box { background: #3b82f6; border-color: #3b82f6; }
  #logPage .dd-item.sel .box::after {
    content: ''; position: absolute; left: 3.5px; top: 0.5px;
    width: 4px; height: 8px; border: solid #fff; border-width: 0 2px 2px 0;
    transform: rotate(45deg);
  }
  #logPage .dd-item .lvl-dot {
    display: inline-block; min-width: 3.2em; margin-right: 4px;
    font-weight: 600; font-size: 10px; text-align: center;
    padding: 1px 5px; border-radius: 3px;
  }

  /* custom dropdown (replaces native <select>; popup always opens right under the button) */
  #logPage .dd { position: relative; min-width: 0; }
  #logPage .dd-btn {
    height: 26px; padding: 0 6px; font-size: 12px; border: 1px solid #d1d5db; border-radius: 4px;
    background: #fff; cursor: pointer; color: #374151; display: flex; align-items: center; gap: 4px;
    box-sizing: border-box; max-width: 150px; min-width: 0;
  }
  #logPage .dd-btn:hover { background: #f3f4f6; border-color: #9ca3af; }
  #logPage .dd-btn .dd-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  #logPage .dd-btn .dd-caret { font-size: 9px; color: #9ca3af; }
  #logPage .dd-menu {
    display: none; position: absolute; top: calc(100% + 3px); left: 0; z-index: 1000;
    min-width: 220px; max-width: 320px;
    background: #fff; border: 1px solid #d1d5db; border-radius: 4px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  }
  #logPage .dd-search { padding: 6px 8px; border-bottom: 1px solid #f1f5f9; }
  #logPage .dd-search input {
    width: 100%; height: 24px; padding: 0 8px; font-size: 12px;
    border: 1px solid #e2e8f0; border-radius: 3px; outline: none; box-sizing: border-box;
  }
  #logPage .dd-search input:focus { border-color: #3b82f6; }
  #logPage .dd-count { padding: 4px 10px 2px; font-size: 10px; color: #94a3b8; }
  #logPage .dd-list { max-height: 260px; overflow-y: auto; padding: 3px 0; }
  #logPage .dd.open .dd-menu { display: block; }
  #logPage .dd-item {
    padding: 4px 10px; font-size: 12px; color: #374151; cursor: pointer;
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  }
  #logPage .dd-item:hover { background: #eff6ff; }
  #logPage .dd-item.sel { background: #dbeafe; color: #1d4ed8; font-weight: 500; }
  #logPage .dd-empty { padding: 4px 10px; font-size: 11px; color: #9ca3af; font-style: italic; }

  /* log area */
  #logPage .logbox {
    border: 1px solid #dee2e6; border-radius: 6px; background: #fff;
    height: calc(100vh - 200px); min-height: 300px;
    overflow-y: auto; padding: 4px 0;
    font-family: "SF Mono", "Fira Code", "Consolas", monospace; font-size: 12px; line-height: 1.7;
  }
  #logPage .entry {
    padding: 3px 12px; cursor: default;
    display: flex; flex-wrap: wrap; align-items: flex-start; gap: 8px;
    border-bottom: 1px solid #f1f5f9;
  }
  #logPage .entry:last-child { border-bottom: none; }
  #logPage .entry.has-stack { cursor: pointer; }
  #logPage .entry:hover { background: #f8fafc; }
  #logPage .badge {
    flex-shrink: 0; min-width: 3em; padding: 1px 6px; border-radius: 3px;
    font-weight: 600; font-size: 10px; text-align: center; line-height: 1.6;
    text-transform: uppercase; letter-spacing: 0.3px;
  }
  #logPage .lvl-TRACE { background: #f1f5f9; color: #64748b; }
  #logPage .lvl-DEBUG { background: #dbeafe; color: #1d4ed8; }
  #logPage .lvl-INFO { background: #dcfce7; color: #15803d; }
  #logPage .lvl-WARN { background: #fef3c7; color: #b45309; }
  #logPage .lvl-ERROR { background: #fee2e2; color: #dc2626; }
  #logPage .ts { flex-shrink: 0; color: #94a3b8; font-size: 11px; line-height: 1.8; }
  #logPage .msg { flex: 1 1 0; min-width: 200px; white-space: pre-wrap; word-break: break-all; color: #334155; line-height: 1.6; }
  #logPage .entry.open .msg { font-weight: 500; color: #1e293b; }
  #logPage .caret {
    flex-shrink: 0; color: #cbd5e1; font-size: 10px; line-height: 1.8;
    transition: transform 0.15s; user-select: none;
  }
  #logPage .entry:hover .caret { color: #94a3b8; }
  #logPage .entry.open .caret { transform: rotate(90deg); color: #64748b; }
  #logPage .stack {
    flex: 1 1 100%; margin: 2px 0 4px 0; padding: 8px 12px;
    background: #f8fafc; border-left: 3px solid #e2e8f0; border-radius: 0 4px 4px 0;
    color: #64748b; font-size: 11px; white-space: pre-wrap; word-break: break-all;
    display: none; line-height: 1.5; box-sizing: border-box;
  }
  #logPage .entry.open .stack { display: block; }

  /* footer */
  #logPage .footer {
    margin-top: 6px; font-size: 11px; color: #94a3b8;
    display: flex; justify-content: space-between; align-items: center;
  }
  #logPage .footer .stats { color: #64748b; }
</style>

<div class="content"><div id="logPage">
    <div class="toolbar">
        <div class="group">
            <span class="status-wrap">
                <span id="statusDot" class="status-dot reconnecting"></span>
                <span id="statusText" class="status-text">connecting</span>
            </span>
            <button id="btnPause" class="btn" title="Pause live updates (events are buffered)">
                <svg viewBox="0 0 16 16"><rect x="3" y="2" width="3.5" height="12" rx="0.5"/><rect x="9.5" y="2" width="3.5" height="12" rx="0.5"/></svg>
                Pause
            </button>
            <button id="btnClear" class="btn" title="Clear the log view">
                <svg viewBox="0 0 16 16"><path d="M4 2l8 12M12 2l-8 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                Clear
            </button>
            <label title="Auto-scroll to newest entries">
                <input type="checkbox" id="followScroll" checked /> Follow
            </label>
        </div>
        <div class="group">
            <div class="dd" id="dateDD">
                <button class="dd-btn" type="button"><span class="dd-label">Today</span><span class="dd-caret">&#9660;</span></button>
                <div class="dd-menu"><div class="dd-list" id="dateList"></div></div>
            </div>
            <div class="dd" id="stationDD">
                <button class="dd-btn" type="button"><span class="dd-label">All stations</span><span class="dd-caret">&#9660;</span></button>
                <div class="dd-menu">
                    <div class="dd-search"><input type="text" id="stationSearch" placeholder="Filter stations..." /></div>
                    <div class="dd-count" id="stationCount"></div>
                    <div class="dd-list" id="stationList"></div>
                </div>
            </div>
            <div class="dd" id="levelDD">
                <button class="dd-btn" type="button"><span class="dd-label">All levels</span><span class="dd-caret">&#9660;</span></button>
                <div class="dd-menu"><div class="dd-list" id="levelList"></div></div>
            </div>
            <input type="text" id="fSearch" placeholder="Search keyword..." />
            <button id="btnPrev" class="btn" title="Previous match">&#9664;</button>
            <button id="btnNext" class="btn" title="Next match">&#9654;</button>

        </div>
        <div class="spacer"></div>
        <div class="group">
            <span id="stats" class="status-text"></span>
        </div>
    </div>

    <div id="authBanner" style="display:none; margin-bottom:8px; padding:8px 12px; background:#fef2f2; border:1px solid #fecaca; border-radius:6px; font-size:12px; color:#991b1b; align-items:center; gap:10px;">
        <span>Login expired &mdash; the page can no longer receive logs.</span>
        <button id="btnRelogin" style="padding:3px 12px; font-size:12px; border:1px solid #fca5a5; border-radius:4px; background:#fff; color:#991b1b; cursor:pointer;">Refresh &amp; sign in</button>
    </div>

    <div class="logbox" id="logBox"></div>

</div></div>

<script type="text/javascript">
(function() {
    var logBox = document.getElementById('logBox');
    var statusDot = document.getElementById('statusDot');
    var statusText = document.getElementById('statusText');
    var btnPause = document.getElementById('btnPause');
    var btnClear = document.getElementById('btnClear');
    var followScroll = document.getElementById('followScroll');
    var fSearch = document.getElementById('fSearch');
    var btnPrev = document.getElementById('btnPrev');
    var btnNext = document.getElementById('btnNext');
    var statsEl = document.getElementById('stats');

    var ws = null;
    var paused = false;
    var buffer = [];
    var BUFFER_MAX = 2000;
    var shown = 0, totalReceived = 0;
    var reconnectDelay = 1000, reconnectTimer = null, RECONNECT_MAX_DELAY = 30000;
    var pingTimer = null, openWatchdog = null;
    var authDead = false, intentionalClose = false;
    var recentEntries = [];
    var matchEls = [], navPos = -1;
    var searchTimer = null;

    var selectedStation = '';
    var stations = {};   // chargeBoxId -> true，从日志动态发现
    var viewingPast = false;
    var currentDate = 'today';
    var selLevels = {INFO: true, WARN: true, ERROR: true};

    var LEVEL_CLASS = {TRACE:'lvl-TRACE', DEBUG:'lvl-DEBUG', INFO:'lvl-INFO', WARN:'lvl-WARN', ERROR:'lvl-ERROR'};


    function setStatus(state, text) {
        statusDot.className = 'status-dot ' + state;
        statusText.textContent = text;
    }

    function effectiveKeyword() {
        return selectedStation || fSearch.value.trim();
    }

    // ---------- 过滤：级别 chips + 关键词（桩号或搜索词，单通道） ----------
    function passesClient(e) {
        if (!selLevels[e.level]) return false;
        var kw = effectiveKeyword();
        if (kw && (e.message || '').indexOf(kw) < 0) return false;
        return true;
    }

    function currentFilters() {
        var levels = [];
        ['INFO', 'WARN', 'ERROR'].forEach(function(l) { if (selLevels[l]) levels.push(l); });
        return { type: 'filters', level: levels.join(','), contains: effectiveKeyword() };
    }

    function sendFilters() {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify(currentFilters()));
        }
    }

    // ---------- 渲染 ----------
    // 服务端 Encode.forHtml 转义后的消息在此解码。必须用 innerHTML（textarea 的
    // RCDATA 上下文会解码实体但不解析标签，分离节点无 XSS 面）；textContent 是
    // 纯文本赋值不解码实体，曾导致 &#34; 等实体原样显示。
    var decodeBuf = document.createElement('textarea');
    function decodeEntities(s) {
        decodeBuf.innerHTML = s;
        var v = decodeBuf.value;
        decodeBuf.innerHTML = '';
        return v;
    }

    function fillHighlighted(el, text, kw) {
        if (!kw) { el.textContent = text; return; }
        var lower = text.toLowerCase(), k = kw.toLowerCase(), i = 0, hit;
        while ((hit = lower.indexOf(k, i)) >= 0) {
            if (hit > i) el.appendChild(document.createTextNode(text.substring(i, hit)));
            var mk = document.createElement('mark');
            mk.className = 'kw';
            mk.textContent = text.substring(hit, hit + k.length);
            el.appendChild(mk);
            i = hit + k.length;
        }
        if (i < text.length) el.appendChild(document.createTextNode(text.substring(i)));
    }

    function makeEntry(e) {
        var div = document.createElement('div');
        var hasStack = e.message && e.message.indexOf('\n') >= 0;
        div.className = 'entry' + (hasStack ? ' has-stack' : '');

        var badge = document.createElement('span');
        badge.className = 'badge ' + (LEVEL_CLASS[e.level] || 'lvl-INFO');
        badge.textContent = e.level;
        div.appendChild(badge);

        if (e.ts) {
            var ts = document.createElement('span');
            ts.className = 'ts';
            ts.textContent = e.ts;
            div.appendChild(ts);
        }

        var msgText = decodeEntities(e.message || '');
        var dashIdx = msgText.indexOf(' - ');
        if (dashIdx > 0) { msgText = msgText.substring(dashIdx + 3); }
        var nl = msgText.indexOf('\n');
        var firstLine = nl >= 0 ? msgText.substring(0, nl) : msgText;
        var stackText = nl >= 0 ? msgText.substring(nl + 1) : '';

        var msg = document.createElement('span');
        msg.className = 'msg';
        fillHighlighted(msg, firstLine, effectiveKeyword());
        div.appendChild(msg);

        if (stackText) {
            var caret = document.createElement('span');
            caret.className = 'caret';
            caret.textContent = '\u25B8';
            div.appendChild(caret);

            var stack = document.createElement('div');
            stack.className = 'stack';
            fillHighlighted(stack, stackText, effectiveKeyword());
            div.appendChild(stack);
            div.addEventListener('click', function() { div.classList.toggle('open'); });
        }
        return div;
    }

    // ---------- 搜索导航 ----------
    function collectMatches() {
        var kw = effectiveKeyword().toLowerCase();
        matchEls = [];
        if (!kw) return;
        Array.prototype.forEach.call(logBox.children, function(el) {
            if (el.textContent.toLowerCase().indexOf(kw) >= 0) matchEls.push(el);
        });
    }

    function jumpToMatch(i) {
        if (!matchEls.length) return;
        var el = matchEls[(i + matchEls.length) % matchEls.length];
        el.scrollIntoView({ block: 'center' });
        el.classList.remove('flash');
        void el.offsetWidth;
        el.classList.add('flash');
        setTimeout(function() { el.classList.remove('flash'); }, 1200);
    }

    function gotoMatch(dir) {
        collectMatches();
        if (!matchEls.length) { navPos = -1; updateStats(); return; }
        navPos = (navPos + dir + matchEls.length) % matchEls.length;
        jumpToMatch(navPos);
        updateStats();
    }

    function appendEntry(e) {
        var div = makeEntry(e);
        logBox.appendChild(div);
        shown++;
        while (logBox.children.length > 3000) { logBox.removeChild(logBox.firstChild); shown--; }
        if (followScroll.checked) logBox.scrollTop = logBox.scrollHeight;
    }

    // ---------- 批量渲染（性能：DocumentFragment + 单次滚动） ----------
    function renderAll() {
        logBox.innerHTML = '';
        shown = 0;
        var frag = document.createDocumentFragment();
        recentEntries.forEach(function(e) {
            if (!passesClient(e)) return;
            var div = makeEntry(e);
            frag.appendChild(div);
            shown++;
        });
        logBox.appendChild(frag);
        if (followScroll.checked) logBox.scrollTop = logBox.scrollHeight;
        refreshSearchNav();
        updateStats();
    }

    function onEntry(e) {
        totalReceived++;
        if (paused) {
            buffer.push(e);
            if (buffer.length > BUFFER_MAX) { buffer.shift(); }
        } else {
            appendEntry(e);
        }
        updateStats();
    }

    function onSnapshot(entries, date) {
        recentEntries = entries || [];
        viewingPast = (date && date !== 'today');
        renderAll();
        totalReceived = shown;
        updateStats();
    }

    function updateStats() {
        var t = shown + ' shown';
        if (!viewingPast && totalReceived > shown) { t += ' / ' + totalReceived + ' received'; }
        if (paused && buffer.length > 0) { t += ' (+' + buffer.length + ' buffered)'; }
        statsEl.textContent = t;
    }

    // ---------- WS ----------
    function connect() {
        if (authDead) return;
        if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
        openedThisAttempt = false;
        var proto = (location.protocol === 'https:') ? 'wss://' : 'ws://';
        ws = new WebSocket(proto + location.host + '/steve/manager/log/stream');

        if (openWatchdog) clearTimeout(openWatchdog);
        openWatchdog = setTimeout(function() {
            if (ws && ws.readyState === WebSocket.CONNECTING) {
                try { ws.close(); } catch (err) { /* ignore */ }
            }
        }, 8000);

        ws.onopen = function() {
            if (openWatchdog) { clearTimeout(openWatchdog); openWatchdog = null; }
            failCount = 0;
            openedThisAttempt = true;
            reconnectDelay = 1000;
            setStatus('live', 'live');
            sendFilters();
            ws.send(JSON.stringify({type: 'load', date: currentDate}));
            if (pingTimer) clearInterval(pingTimer);
            pingTimer = setInterval(function() {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    ws.send('{"type":"ping"}');
                }
            }, 25000);
        };
        ws.onmessage = function(ev) {
            var data;
            try { data = JSON.parse(ev.data); } catch (err) { return; }
            if (data.type === 'snapshot') {
                if (data.date === currentDate) { onSnapshot(data.entries || []); }
            }
            else if (data.type === 'entry') { onEntry(data); }
        };
        ws.onclose = function() {
            if (pingTimer) { clearInterval(pingTimer); pingTimer = null; }
            if (intentionalClose) { intentionalClose = false; return; }
            if (authDead) return;
            failCount++;
            if (!openedThisAttempt && failCount >= 3) { probeAuth(); return; }
            setStatus('reconnecting', 'reconnecting');
            reconnectTimer = setTimeout(connect, reconnectDelay);
            reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_DELAY);
        };
        ws.onerror = function() {
            try { ws.close(); } catch (err) { /* ignore */ }
        };
    }

    function probeAuth() {
        fetch('/steve/manager/home', {credentials: 'same-origin', redirect: 'manual'})
            .then(function(r) {
                var kicked = r.type === 'opaqueredirect' || r.status === 302 || r.status === 401
                    || r.redirected === true || (r.url && r.url.indexOf('signin') >= 0);
                if (kicked) {
                    authDead = true;
                    setStatus('reconnecting', 'login expired');
                    document.getElementById('authBanner').style.display = 'flex';
                } else {
                    setStatus('reconnecting', 'reconnecting');
                    reconnectTimer = setTimeout(connect, reconnectDelay);
                    reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_DELAY);
                }
            })
            .catch(function() {
                setStatus('reconnecting', 'reconnecting');
                reconnectTimer = setTimeout(connect, reconnectDelay);
                reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_DELAY);
            });
    }

    document.getElementById('btnRelogin').addEventListener('click', function() {
        location.reload();
    });

    // ---------- 控件 ----------
    btnPause.addEventListener('click', function() {
        paused = !paused;
        btnPause.innerHTML = paused ? '&#9654; Resume' : '&#10074;&#10074; Pause';
        setStatus(paused ? 'paused' : 'live', paused ? 'paused' : 'live');
        if (!paused && buffer.length > 0) {
            buffer.forEach(appendEntry);
            buffer = [];
        }
        updateStats();
    });

    btnClear.addEventListener('click', function() {
        logBox.innerHTML = ''; shown = 0; matchEls = []; navPos = -1; updateStats();
    });

    btnPrev.addEventListener('click', function() { gotoMatch(-1); });
    btnNext.addEventListener('click', function() { gotoMatch(1); });

    var searchTimer = null;
    fSearch.addEventListener('input', function() {
        if (selectedStation && fSearch.value.trim() !== selectedStation) {
            selectedStation = '';
            document.getElementById('stationDD').querySelector('.dd-label').textContent = 'All stations';
            sendFilters();
        }
        clearTimeout(searchTimer);
        searchTimer = setTimeout(function() { renderAll(); }, 150);
    });

    // 级别多选下拉（levelDD）
    function refreshLevelMenu() {
        var list = document.getElementById('levelList');
        list.innerHTML = '';
        ['INFO', 'WARN', 'ERROR'].forEach(function(lvl) {
            var item = document.createElement('div');
            item.className = 'dd-item' + (selLevels[lvl] ? ' sel' : '');
            var box = document.createElement('span');
            box.className = 'box';
            item.appendChild(box);
            var badge = document.createElement('span');
            badge.className = 'badge ' + (LEVEL_CLASS[lvl] || 'lvl-INFO');
            badge.textContent = lvl;
            item.appendChild(badge);
            item.addEventListener('click', function(ev) {
                ev.stopPropagation();
                selLevels[lvl] = !selLevels[lvl];
                document.getElementById('levelDD').querySelector('.dd-label').textContent =
                    (selLevels.INFO || selLevels.WARN || selLevels.ERROR) ? 'Levels' : 'None';
                refreshLevelMenu();
                sendFilters();
                renderAll();
            });
            list.appendChild(item);
        });
    }
    bindDropdownToggle('levelDD');
    refreshLevelMenu();

    // ---------- 下拉 ----------
    function closeAllDropdowns(except) {
        document.querySelectorAll('#logPage .dd.open').forEach(function(dd) {
            if (dd !== except) dd.classList.remove('open');
        });
    }
    document.addEventListener('click', function(ev) {
        if (!ev.target.closest('#logPage .dd')) closeAllDropdowns(null);
    });
    function bindDropdownToggle(rootId) {
        var root = document.getElementById(rootId);
        root.querySelector('.dd-btn').onclick = function(ev) {
            ev.stopPropagation();
            var willOpen = !root.classList.contains('open');
            closeAllDropdowns(null);
            if (willOpen) root.classList.add('open');
        };
    }

    // 日期
    var dateItems = [];
    function refreshDateMenu() {
        var list = document.getElementById('dateList');
        list.innerHTML = '';
        dateItems.forEach(function(d) {
            var item = document.createElement('div');
            item.className = 'dd-item' + (d.value === currentDate ? ' sel' : '');
            item.textContent = d.label;
            item.addEventListener('click', function(ev) {
                ev.stopPropagation();
                setDate(d.value);
                closeAllDropdowns(null);
            });
            list.appendChild(item);
        });
    }
    function setDate(v) {
        var changed = (currentDate !== v);
        currentDate = v;
        document.getElementById('dateDD').querySelector('.dd-label').textContent =
            (v === 'today') ? 'Today' : v;
        viewingPast = (v !== 'today');
        if (changed) {
            logBox.innerHTML = ''; shown = 0; recentEntries = []; updateStats();
        }
        refreshDateMenu();
        if (changed && ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({type: 'load', date: v}));
        }
    }
    bindDropdownToggle('dateDD');
    refreshDateMenu();

    fetch('/steve/manager/log/dates', {credentials: 'same-origin'})
        .then(function(r) { return r.json(); })
        .then(function(dates) {
            dateItems = [{value: 'today', label: 'Today'}];
            (dates || []).forEach(function(d) { dateItems.push({value: d, label: d}); });
            refreshDateMenu();
        })
        .catch(function() { /* today-only is fine */ });

    // 桩号
    var stationSearch = document.getElementById('stationSearch');
    function stationItems() {
        var items = [{value: '', label: 'All stations'}];
        Object.keys(stations).sort().forEach(function(id) {
            items.push({value: id, label: id});
        });
        return items;
    }
    function refreshStationMenu() {
        var items = stationItems();
        var kw = stationSearch.value.trim().toLowerCase();
        var listRoot = document.getElementById('stationList');
        listRoot.innerHTML = '';
        var visible = 0;
        items.forEach(function(it) {
            if (kw && it.label.toLowerCase().indexOf(kw) < 0 && it.value !== '') return;
            if (it.value !== '') visible++;
            var item = document.createElement('div');
            item.className = 'dd-item' + (it.value === selectedStation ? ' sel' : '');
            item.textContent = it.label;
            item.onclick = function(ev) {
                ev.stopPropagation();
                selectedStation = it.value;
                document.getElementById('stationDD').querySelector('.dd-label').textContent =
                    it.value === '' ? 'All stations' : it.value;
                closeAllDropdowns(null);
                sendFilters();
                renderAll();
            };
            listRoot.appendChild(item);
        });
        document.getElementById('stationCount').textContent =
            visible + ' / ' + (items.length - 1) + ' stations';
    }
    bindDropdownToggle('stationDD');
    stationSearch.addEventListener('input', refreshStationMenu);
    document.getElementById('stationDD').querySelector('.dd-btn').addEventListener('click', function() {
        stationSearch.value = '';
        refreshStationMenu();
    });
    refreshStationMenu();

    fetch('/steve/manager/log/stations', {credentials: 'same-origin'})
        .then(function(r) { return r.json(); })
        .then(function(list) {
            (list || []).forEach(function(id) { stations[id] = true; });
            refreshStationMenu();
        })
        .catch(function() { /* live-discovered only */ });

    connect();
    setInterval(updateStats, 2000);
})();
</script>


<%@ include file="00-footer.jsp" %>
