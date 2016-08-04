<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\controllers;

use backend\components\Controller;
use gbksoft\multilingual\controllers\actions\CreateAction;
use gbksoft\multilingual\controllers\actions\DeleteAction;
use gbksoft\multilingual\controllers\actions\UpdateAction;
use gbksoft\multilingual\controllers\actions\ViewAction;
use yii\filters\AccessControl;
use yii\helpers\ArrayHelper;

/**
 * Class MainController
 */
class MainController extends Controller
{
    /**
     * @inheritdoc
     */
    public function behaviors()
    {
        return ArrayHelper::merge(
            parent::behaviors(),
            [
                'access' => [
                    'class' => AccessControl::class,
                    'rules' => [
                        [
                            'actions' => ['view', 'update', 'create', 'delete'],
                            'allow' => true,
                            'roles' => ['@'],
                        ]
                    ],
                ],
            ]
        );
    }

    /**
     * @inheritdoc
     */
    public function actions()
    {
        return [
            'view' => [
                'class' => ViewAction::class,
            ],
            'update' => [
                'class' => UpdateAction::class,
            ],
            'create' => [
                'class' => CreateAction::class,
            ],
            'delete' => [
                'class' => DeleteAction::class,
            ],
        ];
    }
}
