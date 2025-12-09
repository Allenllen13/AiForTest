// 初始化默认视图
window.currentView = 'table';

// 标签页切换
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tabName = btn.dataset.tab;
        
        // 更新按钮状态
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        
        // 更新内容区域
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tabName}-tab`).classList.add('active');
        
        // 清空状态消息
        const statusEl = document.getElementById('link-status');
        if (statusEl) {
            statusEl.textContent = '';
            statusEl.className = 'status-message';
        }
    });
});

// 获取飞书文档内容
document.getElementById('fetch-link-btn')?.addEventListener('click', async () => {
    const link = document.getElementById('feishu-link').value.trim();
    const statusEl = document.getElementById('link-status');
    
    if (!link) {
        statusEl.textContent = '请输入飞书文档链接';
        statusEl.className = 'status-message error';
        return;
    }
    
    statusEl.textContent = '正在获取文档内容...';
    statusEl.className = 'status-message';
    
    try {
        const response = await fetch('/api/feishu/fetch', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ url: link })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            // 将获取的内容填充到文本区域
            document.getElementById('prd-text').value = data.content;
            // 切换到文本标签页
            document.querySelector('[data-tab="text"]').click();
            statusEl.textContent = '文档内容获取成功！';
            statusEl.className = 'status-message success';
        } else {
            statusEl.textContent = data.message || '获取文档内容失败';
            statusEl.className = 'status-message error';
        }
    } catch (error) {
        statusEl.textContent = '网络错误：' + error.message;
        statusEl.className = 'status-message error';
    }
});

// 生成测试用例
document.getElementById('generate-btn').addEventListener('click', async () => {
    const prdText = document.getElementById('prd-text').value.trim();
    
    if (!prdText) {
        alert('请输入或粘贴PRD内容');
        return;
    }
    
    // 显示加载状态
    const loadingEl = document.getElementById('loading');
    const testCasesSection = document.getElementById('test-cases-section');
    const generateBtn = document.getElementById('generate-btn');
    
    loadingEl.classList.remove('hidden');
    testCasesSection.classList.add('hidden');
    generateBtn.disabled = true;
    
    try {
        // 第一步：生成测试用例
        const response = await fetch('/api/testcase/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ prd: prdText })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            // 第二步：自动评测测试用例
            loadingEl.querySelector('p').textContent = '正在评测测试用例，请稍候...';
            try {
                const evalResponse = await fetch('/api/testcase/evaluate', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ 
                        prd: prdText,
                        testCases: data.testCases 
                    })
                });
                
                const evalData = await evalResponse.json();
                
                if (evalResponse.ok && evalData.success) {
                    // 显示测试用例和整体评分
                    displayTestCasesWithEvaluation(data.testCases, evalData.score);
                } else {
                    // 评测失败，只显示测试用例
                    displayTestCases(data.testCases);
                }
            } catch (evalError) {
                console.error('评测失败:', evalError);
                // 评测失败，只显示测试用例
                displayTestCases(data.testCases);
            }
            
            testCasesSection.classList.remove('hidden');
        } else {
            alert('生成测试用例失败：' + (data.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误：' + error.message);
    } finally {
        loadingEl.classList.add('hidden');
        loadingEl.querySelector('p').textContent = '正在生成测试用例，请稍候...';
        generateBtn.disabled = false;
    }
});

// 显示测试用例（带整体评测结果）
function displayTestCasesWithEvaluation(testCases, score) {
    const container = document.getElementById('test-cases-container');
    
    if (!testCases || testCases.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #666; padding: 40px;">未生成测试用例</p>';
        return;
    }
    
    // 保存测试用例和评测结果到全局变量
    window.currentTestCases = testCases;
    window.currentScore = score;
    
    // 显示整体评分
    displayOverallScore(score);
    
    // 根据当前视图模式显示
    const currentView = window.currentView || 'table';
    if (currentView === 'table') {
        displayTableView(testCases, container);
    } else if (currentView === 'card') {
        displayCardView(testCases, container);
    } else if (currentView === 'mindmap') {
        displayMindMapView(testCases, container);
    }
}

// 显示测试用例（不带评测结果，用于评测失败的情况）
function displayTestCases(testCases) {
    const container = document.getElementById('test-cases-container');
    
    if (!testCases || testCases.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #666; padding: 40px;">未生成测试用例</p>';
        return;
    }
    
    // 保存测试用例到全局变量，用于导出
    window.currentTestCases = testCases;
    window.currentScore = null;
    
    // 隐藏评分显示
    const scoreHeader = document.getElementById('overall-score-header');
    if (scoreHeader) {
        scoreHeader.style.display = 'none';
    }
    
    // 根据当前视图模式显示
    const currentView = window.currentView || 'table';
    if (currentView === 'table') {
        displayTableView(testCases, container);
    } else if (currentView === 'card') {
        displayCardView(testCases, container);
    } else if (currentView === 'mindmap') {
        displayMindMapView(testCases, container);
    }
}

// 显示整体评分
function displayOverallScore(score) {
    let scoreHeader = document.getElementById('overall-score-header');
    if (!scoreHeader) {
        scoreHeader = document.createElement('div');
        scoreHeader.id = 'overall-score-header';
        scoreHeader.className = 'overall-score-header';
        const testCasesHeader = document.querySelector('.test-cases-header');
        if (testCasesHeader) {
            testCasesHeader.parentNode.insertBefore(scoreHeader, testCasesHeader.nextSibling);
        }
    }
    if (score) {
        const totalScore = ((score.coverage || 0) + (score.quality || 0) + (score.nonRedundancy || 0)) / 3;
//        ...
//    }
//    if (score && score.totalScore !== undefined) {
//        const totalScore = score.totalScore !== undefined ? score.totalScore :
//            ((score.coverage || 0) + (score.quality || 0) + (score.nonRedundancy || 0)) / 3;
        
        scoreHeader.style.display = 'block';
        scoreHeader.innerHTML = `
            <div class="overall-score-content">
                <div class="overall-score-title">测试用例集合整体评分</div>
                <div class="overall-score-main">
                    <div class="overall-score-total">
                        <span class="score-label">总分：</span>
                        <span class="score-value">${totalScore.toFixed(2)}</span>
                        <span class="score-max">/ 100.00</span>
                    </div>
                    <div class="overall-score-details">
                        <div class="score-detail-item">
                            <span class="detail-label">覆盖性：</span>
                            <span class="detail-value">${score.coverage || 0}</span>
                            <span class="detail-max">/ 100</span>
                        </div>
                        <div class="score-detail-item">
                            <span class="detail-label">质量：</span>
                            <span class="detail-value">${score.quality || 0}</span>
                            <span class="detail-max">/ 100</span>
                        </div>
                        <div class="score-detail-item">
                            <span class="detail-label">非冗余度：</span>
                            <span class="detail-value">${score.nonRedundancy || 0}</span>
                            <span class="detail-max">/ 100</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
    } else {
        scoreHeader.style.display = 'none';
    }
}


// 表格视图（不带评测结果）
function displayTableView(testCases, container) {
    let html = '<table class="test-case-table"><thead><tr>';
    html += '<th style="width: 5%;">序号</th>';
    html += '<th style="width: 20%;">标题</th>';
    html += '<th style="width: 20%;">前置条件</th>';
    html += '<th style="width: 30%;">操作步骤</th>';
    html += '<th style="width: 25%;">预期结果</th>';
    html += '</tr></thead><tbody>';
    
    testCases.forEach((testCase, index) => {
        html += '<tr>';
        html += `<td>${index + 1}</td>`;
        html += `<td class="test-case-title">${escapeHtml(testCase.title || '')}</td>`;
        html += `<td class="test-case-field-content">${formatField(testCase.precondition)}</td>`;
        html += `<td class="test-case-field-content">${formatField(testCase.steps)}</td>`;
        html += `<td class="test-case-field-content">${formatField(testCase.expectedResult)}</td>`;
        html += '</tr>';
    });
    
    html += '</tbody></table>';
    container.innerHTML = html;
}


// 卡片视图（不带评测结果）
function displayCardView(testCases, container) {
    let html = '<div class="test-case-cards">';
    
    testCases.forEach((testCase, index) => {
        html += `<div class="test-case-card">
            <div class="card-header">
                <span class="card-number">${index + 1}</span>
                <h3 class="card-title">${escapeHtml(testCase.title || '')}</h3>
            </div>
            <div class="card-body">
                <div class="card-field">
                    <div class="card-field-label">前置条件：</div>
                    <div class="card-field-content">${formatField(testCase.precondition)}</div>
                </div>
                <div class="card-field">
                    <div class="card-field-label">操作步骤：</div>
                    <div class="card-field-content">${formatField(testCase.steps)}</div>
                </div>
                <div class="card-field">
                    <div class="card-field-label">预期结果：</div>
                    <div class="card-field-content">${formatField(testCase.expectedResult)}</div>
                </div>
            </div>
        </div>`;
    });
    
    html += '</div>';
    container.innerHTML = html;
}

// 思维导图视图
function displayMindMapView(testCases, container) {
    // 使用 Mermaid 生成思维导图
    let mermaidCode = 'mindmap\n  root((测试用例))\n';
    
    testCases.forEach((testCase, index) => {
        const title = (testCase.title || `测试用例${index + 1}`)
            .replace(/[()]/g, '')
            .replace(/"/g, "'")
            .substring(0, 30);
        mermaidCode += `    ${title}\n`;
        
        const precondition = (testCase.precondition || '-')
            .replace(/\n/g, ' ')
            .replace(/"/g, "'")
            .substring(0, 40);
        mermaidCode += `      前置条件: ${precondition}\n`;
        
        const steps = Array.isArray(testCase.steps) 
            ? testCase.steps.join('; ') 
            : (testCase.steps || '-');
        const stepsText = steps.replace(/\n/g, ' ').replace(/"/g, "'").substring(0, 40);
        mermaidCode += `      操作步骤: ${stepsText}\n`;
        
        const expectedResult = (testCase.expectedResult || '-')
            .replace(/\n/g, ' ')
            .replace(/"/g, "'")
            .substring(0, 40);
        mermaidCode += `      预期结果: ${expectedResult}\n`;
    });
    
    container.innerHTML = `<div class="mermaid">${escapeHtml(mermaidCode)}</div>`;
    
    // 初始化 Mermaid
    if (typeof mermaid !== 'undefined') {
        mermaid.initialize({ 
            startOnLoad: false,
            theme: 'default',
            flowchart: {
                useMaxWidth: true,
                htmlLabels: true
            }
        });
        // 使用 setTimeout 确保 DOM 更新后再渲染
        setTimeout(() => {
            mermaid.run({
                querySelector: '.mermaid'
            });
        }, 100);
    } else {
        container.innerHTML = '<p style="text-align: center; color: #666; padding: 40px;">思维导图功能需要加载 Mermaid.js 库，请检查网络连接</p>';
    }
}

// 格式化字段内容（支持字符串和数组）
function formatField(field) {
    if (!field) return '-';
    if (Array.isArray(field)) {
        return field.map((item, idx) => `${idx + 1}. ${escapeHtml(item)}`).join('<br>');
    }
    return escapeHtml(String(field)).replace(/\n/g, '<br>');
}

// HTML转义
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 清空内容
document.getElementById('clear-btn').addEventListener('click', () => {
    if (confirm('确定要清空所有内容吗？')) {
        document.getElementById('prd-text').value = '';
        document.getElementById('feishu-link').value = '';
        document.getElementById('test-cases-section').classList.add('hidden');
        document.getElementById('link-status').textContent = '';
        document.getElementById('link-status').className = 'status-message';
    }
});

// 导出为JSON
document.getElementById('export-btn')?.addEventListener('click', () => {
    if (!window.currentTestCases || window.currentTestCases.length === 0) {
        alert('没有可导出的测试用例');
        return;
    }
    
    const dataStr = JSON.stringify(window.currentTestCases, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `test-cases-${new Date().getTime()}.json`;
    link.click();
    URL.revokeObjectURL(url);
});

// 复制到剪贴板
document.getElementById('copy-btn')?.addEventListener('click', async () => {
    if (!window.currentTestCases || window.currentTestCases.length === 0) {
        alert('没有可复制的测试用例');
        return;
    }
    
    try {
        const text = window.currentTestCases.map((tc, idx) => {
            return `测试用例 ${idx + 1}:\n标题: ${tc.title || ''}\n前置条件: ${formatFieldForText(tc.precondition)}\n操作步骤: ${formatFieldForText(tc.steps)}\n预期结果: ${formatFieldForText(tc.expectedResult)}\n`;
        }).join('\n---\n\n');
        
        await navigator.clipboard.writeText(text);
        alert('测试用例已复制到剪贴板');
    } catch (error) {
        alert('复制失败：' + error.message);
    }
});

function formatFieldForText(field) {
    if (!field) return '-';
    if (Array.isArray(field)) {
        return field.map((item, idx) => `${idx + 1}. ${item}`).join('\n');
    }
    return String(field);
}

// 视图切换
document.querySelectorAll('.view-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const view = btn.dataset.view;
        
        // 更新按钮状态
        document.querySelectorAll('.view-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        
        // 更新视图
        window.currentView = view;
        if (window.currentTestCases) {
            if (window.currentScore) {
                displayTestCasesWithEvaluation(window.currentTestCases, window.currentScore);
            } else {
                displayTestCases(window.currentTestCases);
            }
        }
    });
});

// 导出为Excel（简化版，实际可以使用SheetJS等库）
document.getElementById('export-excel-btn')?.addEventListener('click', () => {
    if (!window.currentTestCases || window.currentTestCases.length === 0) {
        alert('没有可导出的测试用例');
        return;
    }
    
    // 生成CSV格式（Excel可以打开）
    let csv = '序号,标题,前置条件,操作步骤,预期结果\n';
    
    window.currentTestCases.forEach((tc, idx) => {
        const steps = Array.isArray(tc.steps) 
            ? tc.steps.map((s, i) => `${i + 1}. ${s}`).join('; ') 
            : (tc.steps || '-');
        csv += `${idx + 1},"${(tc.title || '').replace(/"/g, '""')}","${(tc.precondition || '-').replace(/"/g, '""')}","${steps.replace(/"/g, '""')}","${(tc.expectedResult || '-').replace(/"/g, '""')}"\n`;
    });
    
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `test-cases-${new Date().getTime()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
});

