/**
 * 枠の位置、大きさのスタイルを構築します。
 * @param {type} basePos 操作ログページでのスクリーンショットの位置
 * @param {type} pos 実行時ページでの項目の位置
 * @returns {String} 表示する枠のスタイル
 */
function buildPosStyle(basePos, pos) {
	return "left:" + (basePos.left + pos.x - 10) + "px;"
		+ "top:" + (basePos.top + pos.y - 10) + "px;"
		+ "width:" + (pos.w + 20) + "px;"
		+ "height:" + (pos.h + 20) + "px;";
}

function buildBox() {
	var posMap = {};

	/**
	 * 同一座標に紐づく操作ログのNoをまとめる処理
	 */
	var checkPos = function(pos, td) {
		var key = pos.x + "_" + pos.y
		var mappedId = posMap[key] ;
		if (mappedId) {
			td.find("#" + mappedId).append("," + pos.no);
			return;
		}
		var id = "numbring_" + pos.no;
		posMap[key] = id;
		return id;
	};

	$("td.screenshot").each(function() {
		var td = $(this);
		var basePos = td.find("img").offset();


		td.find("input:hidden").each(function() {
			var val = $(this).val();
			var pos = $.parseJSON(val);

			var id = checkPos(pos, td);
			if (!id) {
				return;
			}
			var style = buildPosStyle(basePos, pos);

			td.prepend("<div id='" + id + "' class='box' style=" + style + ">" + pos.no + "</div>");
		});
	});
	
	$("div.box").click(function() {
		$(this).toggleClass("box-hidden");
	});
}

$(window).load(function() {
	buildBox();
});

$(window).resize(function() {
	$("div.box").remove();
	buildBox();
});
